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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import de.fibs.geoappandroid.R
import de.fibs.geoappandroid.models.Datapoint
import de.fibs.geoappandroid.repo.DataRepository
import de.fibs.geoappandroid.repo.DataRepository.Companion.DEFAULT_FREQUENCY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime

class LocationStepService : LifecycleService(), SensorEventListener {

    //region Private fields

    private val repo = DataRepository.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastStepCount: Int? = null
    private var lastLocation: Location? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val dataQueue: ArrayList<Datapoint> = ArrayList(repo.bufferSize)
    private var loopJob: Job? = null

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
        repo.frequency.observe(this) { stopLocationUpdates(); startLocationUpdates() }

        // Observe collecting change
        repo.collecting.observe(this) {
            if (repo.collecting.value == true) {
                scheduleUpdates()
            } else {
                stopUpdates()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdates()
    }

    //endregion

    //region Worker coroutine

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun scheduleUpdates() {
        startLocationUpdates()

        // Register for step counter updates
        stepSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        loopJob = scope.launch(Dispatchers.Default) {
           while (isActive) {
               delay((repo.frequency.value ?: DEFAULT_FREQUENCY) * 1000)
               if (repo.collecting.value == true) {
                   sendUpdate()
               }
           }
       }
    }

    private fun stopUpdates() {
        loopJob?.cancel()
        loopJob = null
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    //endregion

    //region Send data

    private suspend fun sendUpdate() {
        putDatapointOnQueue()

        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.2.55:8080/api/data/8c1c59c5-faaf-42c5-88c6-bf80049f1c0f")

                val jsonInputString = Json.encodeToString(dataQueue)

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoiYWRtaW4iLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjhjMWM1OWM1LWZhYWYtNDJjNS04OGM2LWJmODAwNDlmMWMwZiIsIlBlcm1pc3Npb25zIjoiW1wicGVybTpXcml0ZURhdGFcIl0iLCJleHAiOjE3NDgzNTk0NzQsImlzcyI6Ikdlb0FwcCIsImF1ZCI6Ikdlb0FwcCJ9.rNCqpO2Nb5HMG971vHHboTC-r8iMZN4MYukj6EYjhfA")
                connection.doOutput = true

                connection.outputStream.use { os: OutputStream ->
                    val input: ByteArray = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Handle success
                    clearQueue()
                } else {
                    // Handle error
                    Log.e("GeoApp", "Request failed. Status code: $responseCode")
                }
                connection.disconnect()
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
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, (repo.frequency.value ?: DEFAULT_FREQUENCY) * 1000)
            .setMaxUpdateDelayMillis((repo.frequency.value ?: DEFAULT_FREQUENCY) * 500)
            .build()

        // Register for location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun onLocationChanged(locationResult: LocationResult) {
        lastLocation = locationResult.lastLocation
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

    private fun putDatapointOnQueue() {
        val now = OffsetDateTime.now()
        val datapoint = Datapoint(
            lastLocation?.latitude ?: -1.0,
            lastLocation?.longitude ?: -1.0,
            lastStepCount ?: -1,
            now.toString()
        )
        dataQueue.add(datapoint)
        repo.setCurrentBufferSize(dataQueue.size)
    }

    private fun clearQueue() {
        dataQueue.clear()
        repo.setCurrentBufferSize(0)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        return notificationBuilder.build()
    }

    //endregion
}
