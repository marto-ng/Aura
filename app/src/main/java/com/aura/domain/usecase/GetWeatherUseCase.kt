package com.aura.domain.usecase

import com.aura.data.api.WeatherResponse
import com.aura.domain.repository.IWeatherRepository

class GetWeatherUseCase(
    private val repository: IWeatherRepository
) {
    suspend operator fun invoke(
        cityName: String,
        latitude: Double,
        longitude: Double
    ): Result<WeatherResponse> {
        return try {
            val response = repository.getForecast(latitude, longitude)
            repository.saveWeatherLocalCache(cityName, response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
