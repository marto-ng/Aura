package com.example.data.repository

import com.example.data.api.GeocodingResult
import com.example.data.api.WeatherApiClients
import com.example.data.api.WeatherResponse
import com.example.data.db.FavoriteDao
import com.example.data.db.FavoriteLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherRepository(private val favoriteDao: FavoriteDao) {

    val allFavorites: Flow<List<FavoriteLocation>> = favoriteDao.getAllFavorites()

    suspend fun getForecast(latitude: Double, longitude: Double): WeatherResponse {
        return WeatherApiClients.forecastApi.getForecast(
            lat = latitude,
            lon = longitude
        )
    }

    suspend fun searchLocations(query: String): List<GeocodingResult> {
        val response = WeatherApiClients.geocodingApi.searchLocations(name = query)
        return response.results ?: emptyList()
    }

    suspend fun insertFavorite(favorite: FavoriteLocation) {
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(favorite: FavoriteLocation) {
        favoriteDao.deleteFavorite(favorite)
    }

    suspend fun deleteFavoriteByCoordinates(latitude: Double, longitude: Double) {
        favoriteDao.deleteByCoordinates(latitude, longitude)
    }

    fun isFavorite(latitude: Double, longitude: Double): Flow<Boolean> {
        return favoriteDao.isFavoriteExists(latitude, longitude).map { count -> count > 0 }
    }
}
