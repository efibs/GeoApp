package de.fibs.geoappandroid.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import de.fibs.geoappandroid.R
import de.fibs.geoappandroid.models.Datapoint
import de.fibs.geoappandroid.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.util.Date

class LocationStepService : LifecycleService(), SensorEventListener {

    //region Private fields

    private val repo = SettingsRepository.getInstance(null)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastStepCount: Int? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val dataQueueLock = Any()
    private val dataQueue = ArrayList<Datapoint>(repo.bufferSize)
    private var sendDataJob: Job? = null
    private var isSendingData = true
    private var sendFrequency: Long = runBlocking {
        repo.sendFrequency.first()
    }
    private var sensorFrequency: Long = runBlocking {
        repo.sensorFrequency.first()
    }
    private var apiEndpoint: String = runBlocking {
        repo.apiEndpoint.first()
    }
    private var sendToken: String = runBlocking {
        repo.token.first()
    }
    private var userId: String = runBlocking {
        repo.userId.first()
    }

    //endregion

    //region Android lifecycle

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Start foreground service
        val notification = createNotification()
        startForeground(1, notification)

        // Create the location callback
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationChanged(locationResult)
            }
        }

        // Observe frequency change
        lifecycleScope.launch {
            repo.sensorFrequency.collect { newSensorFrequency ->
                sensorFrequency = newSensorFrequency
                if (repo.collecting.value == true) {
                    stopLocationUpdates()
                    startLocationUpdates()
                }}
        }
        lifecycleScope.launch {
            repo.sendFrequency.collect { newSendFrequency ->
                sendFrequency = newSendFrequency
            }
        }

        // Observer api stuff
        lifecycleScope.launch {
            repo.apiEndpoint.collect{ newApiEndpoint ->
                apiEndpoint = newApiEndpoint
            }
        }
        lifecycleScope.launch {
            repo.token.collect{ newSendToken ->
                sendToken = newSendToken
            }
        }
        lifecycleScope.launch {
            repo.userId.collect{newUserId ->
                userId = newUserId
            }
        }

        // Observe collecting change
        repo.collecting.observe(this) {
            if (repo.collecting.value == true) {
                scheduleSensorUpdates()
                startSending()
            } else {
                stopSensorUpdates()
                stopSending()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorUpdates()
    }

    //endregion

    //region Data sending

    private suspend fun processQueue() {
        while (isSendingData || dataQueue.isNotEmpty()) {
            delay(sendFrequency * 1000)
            trySendUpdate()
        }
    }

    private fun startSending() {
        sendDataJob?.cancel()
        isSendingData = true
        sendDataJob = scope.launch(Dispatchers.Default) {
            processQueue()
        }
    }

    private fun stopSending() {
        isSendingData = false
    }

    //endregion

    //region Sensor updates

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun scheduleSensorUpdates() {
        startLocationUpdates()

        // Register for step counter updates
        stepSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopSensorUpdates() {
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    //endregion

    //region Send data

    private suspend fun trySendUpdate() {
        Log.d("GeoApp", "Trying to send data.")

        withContext(Dispatchers.IO) {
            try {
                val url = URL("${apiEndpoint}/api/data/${userId}")

                Log.d("GeoApp", "Sending to $url with token: $sendToken")

                synchronized(dataQueueLock) {
                    val jsonInputString = Json.encodeToString(dataQueue)

                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 1000
                    connection.readTimeout = 1000
                    connection.requestMethod = "PUT"
                    connection.setRequestProperty("Content-Type", "application/json; utf-8")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $sendToken"
                    )
                    connection.doOutput = true

                    connection.outputStream.use { os: OutputStream ->
                        val input: ByteArray = jsonInputString.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Handle success
                        dataQueue.clear()
                        repo.setCurrentBufferSize(0)
                    } else {
                        // Handle error
                        Log.e("GeoApp", "Request failed. Status code: $responseCode")
                    }
                    connection.disconnect()
                }
            } catch (ex: Exception) {
                Log.e("GeoApp", "Failed to send request.", ex)
            }
        }
    }

    //endregion

    //region Location sensor

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, sensorFrequency * 1000)
            .setMaxUpdateDelayMillis(500)
            .build()

        // Register for location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun onLocationChanged(locationResult: LocationResult) {
        Log.d("GeoApp", "New location received.")

        val lastLocation = locationResult.lastLocation ?: return

        putDatapointOnQueue(lastLocation)
    }

    //endregion

    //region Step sensor

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            lastStepCount = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes
    }

    //endregion

    //region Send buffer

    private fun putDatapointOnQueue(location: Location) {
        val now = OffsetDateTime.now()
        val datapoint = Datapoint(
            location.latitude,
            location.longitude,
            lastStepCount ?: -1,
            now.toString()
        )
        synchronized(dataQueueLock) {
            dataQueue.add(datapoint)
        }
        repo.setCurrentBufferSize(dataQueue.size)
        repo.setLastSensorUpdateTime(Date())
    }

    //endregion

    //region Foreground service boilerplate

    private fun createNotification(): Notification {
        val channelId = "location_step_service_channel"
        val channelName = "Location and Step Tracking Service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Location and Steps")
            .setContentText("Your location and steps are being tracked in the background.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)

        return notificationBuilder.build()
    }

    //endregion
}
