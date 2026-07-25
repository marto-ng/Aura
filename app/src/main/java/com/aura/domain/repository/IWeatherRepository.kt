package com.aura.domain.repository

import com.aura.data.api.GeocodingResult
import com.aura.data.api.WeatherResponse
import com.aura.data.db.FavoriteLocation
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture repository contract.
 */
interface IWeatherRepository {
    val allFavorites: Flow<List<FavoriteLocation>>

    suspend fun getForecast(latitude: Double, longitude: Double): WeatherResponse

    suspend fun searchLocations(query: String): List<GeocodingResult>

    suspend fun insertFavorite(favorite: FavoriteLocation)

    suspend fun deleteFavorite(favorite: FavoriteLocation)

    suspend fun deleteFavoriteByCoordinates(latitude: Double, longitude: Double)

    fun isFavorite(latitude: Double, longitude: Double): Flow<Boolean>

    fun saveWeatherLocalCache(cityName: String, weatherData: WeatherResponse)
}
