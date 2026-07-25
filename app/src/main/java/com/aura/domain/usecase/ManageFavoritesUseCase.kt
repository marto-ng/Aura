package com.aura.domain.usecase

import com.aura.data.db.FavoriteLocation
import com.aura.domain.model.LocationDomainModel
import com.aura.domain.repository.IWeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ManageFavoritesUseCase(
    private val repository: IWeatherRepository
) {
    val allFavorites: Flow<List<FavoriteLocation>> = repository.allFavorites

    fun isFavorite(latitude: Double, longitude: Double): Flow<Boolean> {
        return repository.allFavorites.map { list ->
            list.any { fav ->
                kotlin.math.abs(fav.latitude - latitude) < 0.05 &&
                kotlin.math.abs(fav.longitude - longitude) < 0.05
            }
        }
    }

    suspend fun toggleFavorite(location: LocationDomainModel, currentlyFavorite: Boolean) {
        if (currentlyFavorite) {
            repository.deleteFavoriteByCoordinates(location.latitude, location.longitude)
        } else {
            repository.insertFavorite(
                FavoriteLocation(
                    name = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    country = location.country,
                    admin1 = location.admin1
                )
            )
        }
    }
}
