package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    @Json(name = "timezone_abbreviation") val timezoneAbbreviation: String? = null,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Long? = null,
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    val precipitation: Double,
    val rain: Double,
    val showers: Double,
    val snowfall: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "is_day") val isDay: Int,
    @Json(name = "cloud_cover") val cloudCover: Double,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "wind_direction_10m") val windDirection10m: Double,
    @Json(name = "uv_index") val uvIndex: Double
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperature2m: List<Double>,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Double>,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>,
    val precipitation: List<Double>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double>,
    @Json(name = "uv_index") val uvIndex: List<Double>
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    @Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double>,
    @Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double>
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    @Json(name = "country_code") val countryCode: String?,
    val admin1: String? = null
)
