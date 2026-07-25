package com.aura.domain.model

import com.aura.data.api.WeatherResponse

/**
 * Domain entity wrapping full weather information for a location.
 */
data class WeatherDomainModel(
    val rawResponse: WeatherResponse,
    val timezone: String?,
    val utcOffsetSeconds: Long?,
    val currentTemp: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val rainProbability: Int,
    val humidity: Int?,
    val uvIndex: Double?
)
