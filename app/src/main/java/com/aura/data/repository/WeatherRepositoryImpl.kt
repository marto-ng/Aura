package com.aura.data.repository

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.aura.data.api.GeocodingApi
import com.aura.data.api.GeocodingResult
import com.aura.data.api.WeatherForecastApi
import com.aura.data.api.WeatherResponse
import com.aura.data.db.FavoriteDao
import com.aura.data.db.FavoriteLocation
import com.aura.domain.repository.IWeatherRepository
import com.aura.ui.weather.WeatherAppWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val forecastApi: WeatherForecastApi,
    private val geocodingApi: GeocodingApi,
    private val context: Context
) : IWeatherRepository {

    override val allFavorites: Flow<List<FavoriteLocation>> = favoriteDao.getAllFavorites()

    // Memory cache for battery & antenna power optimization
    private var lastFetchedTime = 0L
    private var lastFetchedLat = 0.0
    private var lastFetchedLon = 0.0
    private var lastFetchedData: WeatherResponse? = null

    override suspend fun getForecast(latitude: Double, longitude: Double): WeatherResponse {
        val now = System.currentTimeMillis()
        val isSameLocation = kotlin.math.abs(latitude - lastFetchedLat) < 0.001 &&
                             kotlin.math.abs(longitude - lastFetchedLon) < 0.001
        val isFresh = (now - lastFetchedTime) < 5 * 60 * 1000 // 5 minutes freshness window

        val cachedData = lastFetchedData
        if (isSameLocation && isFresh && cachedData != null) {
            return cachedData
        }

        return try {
            val response = forecastApi.getForecast(lat = latitude, lon = longitude)
            lastFetchedTime = now
            lastFetchedLat = latitude
            lastFetchedLon = longitude
            lastFetchedData = response
            response
        } catch (e: Exception) {
            val fallback = lastFetchedData
            if (isSameLocation && fallback != null) {
                fallback
            } else {
                throw e
            }
        }
    }

    override suspend fun searchLocations(query: String): List<GeocodingResult> {
        val response = geocodingApi.searchLocations(name = query)
        return response.results ?: emptyList()
    }

    override suspend fun insertFavorite(favorite: FavoriteLocation) {
        favoriteDao.insertFavorite(favorite)
    }

    override suspend fun deleteFavorite(favorite: FavoriteLocation) {
        favoriteDao.deleteFavorite(favorite)
    }

    override suspend fun deleteFavoriteByCoordinates(latitude: Double, longitude: Double) {
        favoriteDao.deleteByCoordinates(latitude, longitude)
    }

    override fun isFavorite(latitude: Double, longitude: Double): Flow<Boolean> {
        return favoriteDao.isFavoriteExists(latitude, longitude).map { count -> count > 0 }
    }

    override fun saveWeatherLocalCache(cityName: String, weatherData: WeatherResponse) {
        try {
            val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
            val currentTemp = weatherData.current?.temperature2m ?: 22.0
            val weatherCode = weatherData.current?.weatherCode ?: 0
            val rainProb = weatherData.hourly?.precipitationProbability?.firstOrNull() ?: 15
            val windSpeed = weatherData.current?.windSpeed10m ?: 0.0
            val uvIndex = weatherData.current?.uvIndex ?: 0.0

            val hourly = weatherData.hourly
            val trendString = if (hourly != null) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
                    val currentHourStr = sdf.format(java.util.Date())
                    val startIdx = hourly.time.indexOfFirst { it.startsWith(currentHourStr.substring(0, 13)) }
                    val startIndexVal = if (startIdx != -1) startIdx else 0
                    val indices = (startIndexVal until (startIndexVal + 24).coerceAtMost(hourly.time.size)).toList()
                    indices.map { hourly.temperature2m.getOrNull(it) ?: currentTemp }.joinToString(",")
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }

            prefs.edit().apply {
                putString("city_name", cityName)
                putFloat("latitude", weatherData.latitude.toFloat())
                putFloat("longitude", weatherData.longitude.toFloat())
                putFloat("temperature", currentTemp.toFloat())
                putInt("weather_code", weatherCode)
                putInt("rain_probability", rainProb)
                putFloat("wind_speed", windSpeed.toFloat())
                putFloat("uv_index", uvIndex.toFloat())
                putString("temp_trend", trendString)
                apply()
            }

            // Update app widget
            val updateIntent = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, WeatherAppWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
