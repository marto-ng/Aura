package com.aura.domain.usecase

import com.aura.data.api.WeatherResponse
import com.aura.domain.repository.IWeatherRepository
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
            val friendlyMessage = formatErrorMessage(e)
            Result.failure(Exception(friendlyMessage, e))
        }
    }

    private fun formatErrorMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException -> "Sin conexión a Internet. Verifica tu Wi-Fi o datos móviles."
            is SocketTimeoutException -> "Tiempo de espera superado. La red está lenta o el servidor no responde."
            is IOException -> "Error de conexión con el servicio meteorológico."
            is retrofit2.HttpException -> when (e.code()) {
                400, 422 -> "Parámetros de ubicación no válidos."
                429 -> "Límite de peticiones alcanzado. Inténtalo de nuevo en unos minutos."
                in 500..599 -> "Servidor de clima en mantenimiento (Error ${e.code()}). Inténtalo más tarde."
                else -> "Error en el servidor de clima (Código ${e.code()})."
            }
            else -> e.localizedMessage?.takeIf { it.isNotBlank() && !it.contains("java.") }
                ?: "No se pudo obtener la información meteorológica. Inténtalo de nuevo."
        }
    }
}
