package com.aura.domain.usecase

import com.aura.data.api.GeocodingResult
import com.aura.domain.repository.IWeatherRepository

class SearchLocationsUseCase(
    private val repository: IWeatherRepository
) {
    suspend operator fun invoke(query: String): List<GeocodingResult> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()
        return try {
            repository.searchLocations(trimmed)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
