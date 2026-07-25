package com.aura.domain.usecase

import com.aura.data.api.GeocodingResult
import com.aura.domain.repository.IWeatherRepository
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SearchLocationsUseCase(
    private val repository: IWeatherRepository
) {
    suspend operator fun invoke(query: String): Result<List<GeocodingResult>> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return Result.success(emptyList())
        return try {
            val results = repository.searchLocations(trimmed)
            Result.success(results)
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is UnknownHostException, is IOException -> "Sin conexión para buscar ubicaciones."
                is SocketTimeoutException -> "Tiempo de espera agotado al buscar."
                else -> "Error al realizar la búsqueda de ubicaciones."
            }
            Result.failure(Exception(errorMsg, e))
        }
    }
}
