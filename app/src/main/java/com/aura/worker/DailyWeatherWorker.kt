package com.aura.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aura.MainActivity
import com.aura.R
import com.aura.data.api.WeatherApiClients
import com.aura.ui.weather.WeatherInfoHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class DailyWeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Try to acquire current device location if permission is granted
            var lat = prefs.getFloat("latitude", 40.4168f).toDouble()
            var lon = prefs.getFloat("longitude", -3.7038f).toDouble()
            var cityName = prefs.getString("city_name", "Madrid") ?: "Madrid"

            if (hasLocationPermission()) {
                val lastLoc = getLastKnownLocation()
                if (lastLoc != null) {
                    lat = lastLoc.first
                    lon = lastLoc.second

                    // Reverse geocode to get current city name
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: cityName
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Fetch current weather from Open-Meteo API
            val weather = WeatherApiClients.forecastApi.getForecast(lat, lon)
            val currentTemp = weather.current?.temperature2m ?: 20.0
            val weatherCode = weather.current?.weatherCode ?: 0
            val windSpeed = weather.current?.windSpeed10m ?: 0.0
            val rainProb = weather.hourly?.precipitationProbability?.firstOrNull() ?: 0
            val maxTemp = weather.daily?.temperature2mMax?.firstOrNull() ?: (currentTemp + 4.0)
            val minTemp = weather.daily?.temperature2mMin?.firstOrNull() ?: (currentTemp - 3.0)

            val weatherDesc = WeatherInfoHelper.getWeatherDescription(weatherCode)

            sendNotification(
                cityName = cityName,
                weatherDesc = weatherDesc,
                currentTemp = currentTemp,
                maxTemp = maxTemp,
                minTemp = minTemp,
                rainProb = rainProb,
                windSpeed = windSpeed
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val finePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return finePerm || coarsePerm
    }

    private suspend fun getLastKnownLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }

    private fun sendNotification(
        cityName: String,
        weatherDesc: String,
        currentTemp: Double,
        maxTemp: Double,
        minTemp: Double,
        rainProb: Int,
        windSpeed: Double
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Resumen Meteorológico Diario",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones diarias matutinas con el resumen del clima."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortTitle = "Buenos días 🌤️ • $cityName"
        val shortContent = "$weatherDesc, ${currentTemp.toInt()}°C (Máx: ${maxTemp.toInt()}°C / Mín: ${minTemp.toInt()}°C). Prob. lluvia: $rainProb%"

        val expandedSummary = """
            📍 Ubicación: $cityName
            🌡️ Clima actual: $weatherDesc (${currentTemp.toInt()}°C)
            📊 Rango de hoy: Máx ${maxTemp.toInt()}°C | Mín ${minTemp.toInt()}°C
            💧 Probabilidad de lluvia: $rainProb%
            💨 Viento: ${windSpeed.toInt()} km/h
        """.trimIndent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(shortTitle)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedSummary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "DailyWeatherSummaryWorker"
        const val CHANNEL_ID = "daily_weather_summary_channel"
        const val NOTIFICATION_ID = 2001
        private const val PREFS_NAME = "weather_widget_prefs"

        /**
         * Schedules periodic daily morning execution using WorkManager.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Calculate initial delay to 07:30 AM next morning
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            if (calendar.get(Calendar.HOUR_OF_DAY) >= 7) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 30)
            calendar.set(Calendar.SECOND, 0)
            val initialDelayMs = (calendar.timeInMillis - now).coerceAtLeast(1000)

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyWeatherWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }

        /**
         * Enqueues an immediate test execution of the worker.
         */
        fun triggerImmediateTest(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val testRequest = OneTimeWorkRequestBuilder<DailyWeatherWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(testRequest)
        }
    }
}
