package com.aura.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.data.api.GeocodingResult
import com.aura.data.api.WeatherResponse
import com.aura.data.db.FavoriteLocation
import com.aura.di.AppContainer
import com.aura.domain.model.LocationDomainModel
import com.aura.domain.usecase.GetGpsLocationUseCase
import com.aura.domain.usecase.GetWeatherUseCase
import com.aura.domain.usecase.ManageFavoritesUseCase
import com.aura.domain.usecase.SearchLocationsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val searchLocationsUseCase: SearchLocationsUseCase,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val getGpsLocationUseCase: GetGpsLocationUseCase
) : ViewModel() {

    // Current viewed location (Defaults to Madrid, Spain)
    private val _currentLocation = MutableStateFlow(LocationDomainModel.DEFAULT)
    val currentLocation: StateFlow<LocationDomainModel> = _currentLocation.asStateFlow()

    // Weather forecast state
    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    // Search Query states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Autocomplete results
    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Temperature unit: true for Celsius, false for Fahrenheit
    private val _isCelsius = MutableStateFlow(true)
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    // List of favorite locations
    val favorites: StateFlow<List<FavoriteLocation>> = manageFavoritesUseCase.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Check if the current viewed location is saved in favorites
    val isCurrentFavorite: StateFlow<Boolean> = _currentLocation.flatMapLatest { loc ->
        manageFavoritesUseCase.isFavorite(loc.latitude, loc.longitude)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private var searchJob: Job? = null

    init {
        fetchWeatherForCurrentLocation()
    }

    fun selectLocation(city: String, country: String, admin1: String?, lat: Double, lon: Double) {
        _currentLocation.value = LocationDomainModel(
            name = city,
            country = country,
            admin1 = admin1,
            latitude = lat,
            longitude = lon
        )
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        fetchWeatherForCurrentLocation()
    }

    fun selectFavorite(fav: FavoriteLocation) {
        selectLocation(
            city = fav.name,
            country = fav.country,
            admin1 = fav.admin1,
            lat = fav.latitude,
            lon = fav.longitude
        )
    }

    fun fetchWeatherForCurrentLocation() {
        viewModelScope.launch {
            val loc = _currentLocation.value
            _weatherState.value = WeatherUiState.Loading

            getWeatherUseCase(loc.name, loc.latitude, loc.longitude)
                .onSuccess { weather ->
                    _weatherState.value = WeatherUiState.Success(weather)
                }
                .onFailure { error ->
                    _weatherState.value = WeatherUiState.Error(
                        error.localizedMessage ?: "Error al obtener la información meteorológica."
                    )
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        val trimmedQuery = newQuery.trim()
        if (trimmedQuery.length < 3) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(350)
            val results = searchLocationsUseCase(trimmedQuery)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val loc = _currentLocation.value
            val currentlyFav = isCurrentFavorite.value
            manageFavoritesUseCase.toggleFavorite(loc, currentlyFav)
        }
    }

    fun toggleTemperatureUnit() {
        _isCelsius.value = !_isCelsius.value
    }

    fun loadWeatherFromGps() {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            val gpsLocation = getGpsLocationUseCase()
            if (gpsLocation != null) {
                _currentLocation.value = gpsLocation
                fetchWeatherForCurrentLocation()
            } else {
                fetchWeatherForCurrentLocation()
            }
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WeatherViewModel(
                        getWeatherUseCase = appContainer.getWeatherUseCase,
                        searchLocationsUseCase = appContainer.searchLocationsUseCase,
                        manageFavoritesUseCase = appContainer.manageFavoritesUseCase,
                        getGpsLocationUseCase = appContainer.getGpsLocationUseCase
                    ) as T
                }
            }
    }
}
