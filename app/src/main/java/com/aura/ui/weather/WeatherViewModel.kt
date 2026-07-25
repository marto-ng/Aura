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

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Transient user message / snackbar notifications
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

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

    fun clearUserMessage() {
        _userMessage.value = null
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
        _searchError.value = null
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
                        error.message ?: "Error al obtener la información meteorológica."
                    )
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        _searchError.value = null

        val trimmedQuery = newQuery.trim()
        if (trimmedQuery.length < 3) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(350)
            val result = searchLocationsUseCase(trimmedQuery)
            result.onSuccess { results ->
                _searchResults.value = results
                _searchError.value = null
            }.onFailure { error ->
                _searchResults.value = emptyList()
                _searchError.value = error.message ?: "Error al buscar ubicaciones."
            }
            _isSearching.value = false
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val loc = _currentLocation.value
                val currentlyFav = isCurrentFavorite.value
                manageFavoritesUseCase.toggleFavorite(loc, currentlyFav)
                _userMessage.value = if (currentlyFav) {
                    "${loc.name} eliminada de favoritos"
                } else {
                    "⭐ ${loc.name} guardada en favoritos"
                }
            } catch (e: Exception) {
                _userMessage.value = "Error al actualizar favoritos: ${e.localizedMessage}"
            }
        }
    }

    fun toggleTemperatureUnit() {
        _isCelsius.value = !_isCelsius.value
    }

    fun loadWeatherFromGps() {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                val gpsLocation = getGpsLocationUseCase()
                if (gpsLocation != null) {
                    _currentLocation.value = gpsLocation
                    _userMessage.value = "📍 Ubicación actualizada: ${gpsLocation.name}"
                    fetchWeatherForCurrentLocation()
                } else {
                    _userMessage.value = "Ubicación GPS no disponible. Mostrando ciudad predeterminada."
                    fetchWeatherForCurrentLocation()
                }
            } catch (e: Exception) {
                _userMessage.value = "Error al obtener la ubicación GPS."
                fetchWeatherForCurrentLocation()
            }
        }
    }

    fun resetToDefaultLocation() {
        selectLocation(
            city = LocationDomainModel.DEFAULT.name,
            country = LocationDomainModel.DEFAULT.country,
            admin1 = LocationDomainModel.DEFAULT.admin1,
            lat = LocationDomainModel.DEFAULT.latitude,
            lon = LocationDomainModel.DEFAULT.longitude
        )
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
