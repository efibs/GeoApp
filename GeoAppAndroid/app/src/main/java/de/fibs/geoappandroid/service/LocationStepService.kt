package de.fibs.geoappandroid.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import de.fibs.geoappandroid.R
import de.fibs.geoappandroid.models.Datapoint
import de.fibs.geoappandroid.repo.DataRepository
import de.fibs.geoappandroid.repo.DataRepository.Companion.DEFAULT_FREQUENCY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime

class LocationStepService : Service(), SensorEventListener {

    private val repo = DataRepository.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastStepCount: Int? = null
    private var lastLocation: Location? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Start foreground service
        val notification = createNotification()
        startForeground(1, notification)

        // Register for step counter updates
        stepSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Schedule location and step count updates every minute
        scheduleUpdates()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun onUpdate() {
        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    lastLocation = task.result
                }

                runBlocking {
                    sendUpdate()
                }
            }
    }

    private suspend fun sendUpdate() {
        val now = OffsetDateTime.now()

        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.2.55:8080/api/data/8c1c59c5-faaf-42c5-88c6-bf80049f1c0f")

                val datapoints = arrayOf(
                    Datapoint(
                        lastLocation?.latitude ?: -1.0,
                        lastLocation?.longitude ?: -1.0,
                        lastStepCount ?: -1,
                        now.toString()
                    )
                )
                val jsonInputString = Json.encodeToString(datapoints)

                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoiYWRtaW4iLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjhjMWM1OWM1LWZhYWYtNDJjNS04OGM2LWJmODAwNDlmMWMwZiIsIlBlcm1pc3Npb25zIjoiW1wicGVybTpXcml0ZURhdGFcIl0iLCJleHAiOjE3NDgzNTk0NzQsImlzcyI6Ikdlb0FwcCIsImF1ZCI6Ikdlb0FwcCJ9.rNCqpO2Nb5HMG971vHHboTC-r8iMZN4MYukj6EYjhfA")
                connection.doOutput = true

                connection.outputStream.use { os: OutputStream ->
                    Log.d("GeoApp", "Sending data: $jsonInputString")
                    val input: ByteArray = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Handle success
                    Log.d("GeoApp", "Request sent.")
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

    private fun scheduleUpdates() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

       scope.launch(Dispatchers.Default) {
           while (!job.isCancelled) {
               onUpdate()
               delay((repo.frequency.value ?: DEFAULT_FREQUENCY) * 1000)
           }
       }
    }

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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            lastStepCount = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        job.cancel()
    }
}
