package com.aura.di

import android.content.Context
import com.aura.data.api.WeatherApiClients
import com.aura.data.db.ClimaDatabase
import com.aura.data.repository.WeatherRepositoryImpl
import com.aura.domain.repository.IWeatherRepository
import com.aura.domain.usecase.GetGpsLocationUseCase
import com.aura.domain.usecase.GetWeatherUseCase
import com.aura.domain.usecase.ManageFavoritesUseCase
import com.aura.domain.usecase.SearchLocationsUseCase

/**
 * Dependency Injection Container for Clean Architecture layer provision.
 * Maintains singleton instances across the application lifecycle.
 */
class AppContainer(val context: Context) {

    private val database: ClimaDatabase by lazy {
        ClimaDatabase.getDatabase(context)
    }

    val weatherRepository: IWeatherRepository by lazy {
        WeatherRepositoryImpl(
            favoriteDao = database.favoriteDao(),
            forecastApi = WeatherApiClients.forecastApi,
            geocodingApi = WeatherApiClients.geocodingApi,
            context = context
        )
    }

    val getWeatherUseCase: GetWeatherUseCase by lazy {
        GetWeatherUseCase(weatherRepository)
    }

    val searchLocationsUseCase: SearchLocationsUseCase by lazy {
        SearchLocationsUseCase(weatherRepository)
    }

    val manageFavoritesUseCase: ManageFavoritesUseCase by lazy {
        ManageFavoritesUseCase(weatherRepository)
    }

    val getGpsLocationUseCase: GetGpsLocationUseCase by lazy {
        GetGpsLocationUseCase(context)
    }
}
