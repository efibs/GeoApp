package de.fibs.geoappandroid.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.OffsetDateTime
import java.util.Date
import java.util.concurrent.TimeUnit
import java.net.SocketTimeoutException

class LocationStepService : LifecycleService(), SensorEventListener {

    //region Private fields

    private val ongoingTrackNotificationId = 3141
    private lateinit var repo: SettingsRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastStepCount: Int? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val dataQueueLock = Any()
    private lateinit var dataQueue: ArrayList<Datapoint>
    private var sendDataJob: Job? = null
    private var isSendingData = true
    private var sendFrequency: Long = SettingsRepository.DEFAULT_SEND_FREQUENCY
    private var sensorFrequency: Long = SettingsRepository.DEFAULT_SENSOR_FREQUENCY
    private lateinit var apiEndpoint: String
    private lateinit var sendToken: String
    private lateinit var userId: String
    private var httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(SettingsRepository.DEFAULT_CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .readTimeout(SettingsRepository.DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .writeTimeout(SettingsRepository.DEFAULT_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    //endregion

    //region Android lifecycle

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()

        // Start foreground service
        val notification = createReadyNotification()
        startForeground(1, notification)

        initProperties()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onNotificationDismissedReceiver,
                IntentFilter("DISMISSED_ACTION"),
                RECEIVER_NOT_EXPORTED // This is required on Android 14
            )
        }

        listenToRepoChanges()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorUpdates()
        unregisterReceiver(onNotificationDismissedReceiver)
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
                val url = "${apiEndpoint}/api/data/${userId}"

                Log.d("GeoApp", "Sending to $url with token: $sendToken")

                synchronized(dataQueueLock) {
                    val jsonInputString = Json.encodeToString(dataQueue)
                    val requestBody = jsonInputString.toRequestBody(jsonMediaType)

                    val request = Request.Builder()
                        .url(url)
                        .put(requestBody)
                        .addHeader("Authorization", "Bearer $sendToken")
                        .build()

                    val startTime = System.currentTimeMillis()
                    try {
                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                dataQueue.clear()
                                repo.setCurrentBufferSize(0)
                                repo.setSendErrorText(null)
                            } else {
                                repo.setSendErrorText(response.message)
                                Log.e("GeoApp", "Request failed. Status code: ${response.code}")
                            }
                        }
                    } catch (tex: SocketTimeoutException) {
                        repo.setSendErrorText(tex.cause?.message ?: tex.message)
                        Log.e("GeoApp", "Failed to send request due to timeout.", tex)
                    } catch (ex: Exception) {
                        repo.setSendErrorText(ex.message)
                        Log.e("GeoApp", "Failed to send request due to network error.", ex)
                    }
                    val endTime = System.currentTimeMillis()
                    repo.setLastRequestDurationMillis(endTime - startTime)
                }
            } catch (ex: Exception) {
                repo.setSendErrorText(ex.message)
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

    //region Notifications

    private val onNotificationDismissedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            sendPollingNotification()
        }
    }

    private fun sendPollingNotification() {
        val channelId = "location_step_service_channel"

        val dismissedIntent = Intent("DISMISSED_ACTION")
        dismissedIntent.setPackage(packageName)
        val dismissedPendingIntent = PendingIntent.getBroadcast(this, ongoingTrackNotificationId, dismissedIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Location and Steps")
            .setContentText("Your location and steps are being tracked in the background.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setDeleteIntent(dismissedPendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ongoingTrackNotificationId, notificationBuilder.build())
    }

    private fun removePollingNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ongoingTrackNotificationId)
    }

    private fun createReadyNotification(): Notification {
        val channelId = "location_step_service_channel"
        val channelName = "Location and Step Tracking Service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Location and Steps")
            .setContentText("Service is ready.")
            .setSmallIcon(R.mipmap.ic_launcher)

        return notificationBuilder.build()
    }

    //endregion

    //region Http client

    private fun updateHttpClient(connectTimeoutMillis: Long, readTimeoutMillis: Long, writeTimeoutMillis: Long) {
        val oldClient = httpClient

        val newClient = oldClient.newBuilder()
            .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()

        httpClient = newClient

        oldClient.dispatcher.executorService.shutdown()
        oldClient.connectionPool.evictAll()
        oldClient.cache?.close()

        Log.d("GeoApp", "HTTP Client changed: connectTimeout=$connectTimeoutMillis writeTimeout=$writeTimeoutMillis readTimeout=$readTimeoutMillis")
    }

    //endregion

    //region Repo changes

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun listenToRepoChanges() {
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
                sendPollingNotification()
            } else {
                stopSensorUpdates()
                stopSending()
                removePollingNotification()
            }
        }

        // Observe timeout changes
        repo.httpClientConnectTimeoutMillis.observe(this) {
            updateHttpClient(
                repo.httpClientConnectTimeoutMillis.value ?: SettingsRepository.DEFAULT_CONNECTION_TIMEOUT_MILLIS,
                repo.httpClientReadTimeoutMilliseconds.value ?: SettingsRepository.DEFAULT_READ_TIMEOUT_MILLIS,
                repo.httpClientWriteTimeoutMilliseconds.value ?: SettingsRepository.DEFAULT_WRITE_TIMEOUT_MILLIS
            )
        }
        repo.httpClientWriteTimeoutMilliseconds.observe(this) {
            updateHttpClient(
                repo.httpClientConnectTimeoutMillis.value ?: SettingsRepository.DEFAULT_CONNECTION_TIMEOUT_MILLIS,
                repo.httpClientReadTimeoutMilliseconds.value ?: SettingsRepository.DEFAULT_READ_TIMEOUT_MILLIS,
                repo.httpClientWriteTimeoutMilliseconds.value ?: SettingsRepository.DEFAULT_WRITE_TIMEOUT_MILLIS
            )
        }
        repo.httpClientReadTimeoutMilliseconds.observe(this) {
            updateHttpClient(
                repo.httpClientConnectTimeoutMillis.value ?: SettingsRepository.DEFAULT_CONNECTION_TIMEOUT_MILLIS,
                repo.httpClientReadTimeoutMilliseconds.value ?: SettingsRepository.DEFAULT_READ_TIMEOUT_MILLIS,
                repo.httpClientWriteTimeoutMilliseconds.value ?: SettingsRepository.DEFAULT_WRITE_TIMEOUT_MILLIS
            )
        }
    }

    //endregion

    //region Init properties

    private fun initProperties() {
        repo = SettingsRepository.getInstance(null)

        sendFrequency = runBlocking {
            repo.sendFrequency.first()
        }
        sensorFrequency = runBlocking {
            repo.sensorFrequency.first()
        }
        apiEndpoint = runBlocking {
            repo.apiEndpoint.first()
        }
        sendToken = runBlocking {
            repo.token.first()
        }
        userId = runBlocking {
            repo.userId.first()
        }

        dataQueue = ArrayList(repo.bufferSize)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Create the location callback
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationChanged(locationResult)
            }
        }
    }

    //endregion
}
