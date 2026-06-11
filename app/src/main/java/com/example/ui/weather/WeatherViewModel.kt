package com.example.ui.weather

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeocodingResult
import com.example.data.api.WeatherResponse
import com.example.data.db.ClimaDatabase
import com.example.data.db.FavoriteLocation
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

data class LoadedLocation(
    val name: String,
    val country: String,
    val admin1: String? = null,
    val latitude: Double,
    val longitude: Double
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClimaDatabase.getDatabase(application)
    private val repository = WeatherRepository(db.favoriteDao())

    // Current viewed location (Defaults to Madrid, Spain)
    private val _currentLocation = MutableStateFlow(
        LoadedLocation("Madrid", "España", "Madrid", 40.4168, -3.7038)
    )
    val currentLocation: StateFlow<LoadedLocation> = _currentLocation.asStateFlow()

    // Weather forecast states
    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    // Search Query states for Geocoding Autocomplete
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

    // List of favorite locations from database
    val favorites: StateFlow<List<FavoriteLocation>> = repository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Check if the current viewed location is saved in favorites
    val isCurrentFavorite: StateFlow<Boolean> = combine(favorites, _currentLocation) { favList, current ->
        favList.any { fav ->
            val latDiff = kotlin.math.abs(fav.latitude - current.latitude)
            val lonDiff = kotlin.math.abs(fav.longitude - current.longitude)
            latDiff < 0.05 && lonDiff < 0.05 // safe margin for same location
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private var searchJob: Job? = null

    init {
        // Load initial weather for default location
        fetchWeatherForCurrentLocation()
    }

    fun selectLocation(city: String, country: String, admin1: String?, lat: Double, lon: Double) {
        _currentLocation.value = LoadedLocation(
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
            _weatherState.value = WeatherUiState.Loading
            try {
                val loc = _currentLocation.value
                val weatherData = repository.getForecast(loc.latitude, loc.longitude)
                _weatherState.value = WeatherUiState.Success(weatherData)
                saveWeatherLocalCache(loc.name, weatherData)
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error(
                    e.localizedMessage ?: "Error al intentar obtener la información del clima."
                )
            }
        }
    }

    private fun saveWeatherLocalCache(cityName: String, weatherData: WeatherResponse) {
        try {
            val context = getApplication<Application>().applicationContext
            val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
            val currentTemp = weatherData.current?.temperature2m ?: 22.0
            val weatherCode = weatherData.current?.weatherCode ?: 0
            val rainProb = weatherData.hourly?.precipitationProbability?.firstOrNull() ?: 15
            val windSpeed = weatherData.current?.windSpeed10m ?: 0.0
            val uvIndex = weatherData.current?.uvIndex ?: 0.0
            
            // Extract the 24-hour trend starting from the current hour
            val hourly = weatherData.hourly
            val trendString = if (hourly != null) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
                    val currentHourStr = sdf.format(java.util.Date())
                    val startIdx = hourly.time.indexOfFirst { it.startsWith(currentHourStr.substring(0, 13)) }
                    val startIndexVal = if (startIdx != -1) startIdx else 0
                    val indices = (startIndexVal until (startIndexVal + 24).coerceAtMost(hourly.time.size)).toList()
                    indices.map { hourly.temperature2m.getOrNull(it) ?: currentTemp }.joinToString(",")
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }
            
            prefs.edit().apply {
                putString("city_name", cityName)
                putFloat("temperature", currentTemp.toFloat())
                putInt("weather_code", weatherCode)
                putInt("rain_probability", rainProb)
                putFloat("wind_speed", windSpeed.toFloat())
                putFloat("uv_index", uvIndex.toFloat())
                putString("temp_trend", trendString)
                apply()
            }
            
            // Send update broadcast
            val updateIntent = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, WeatherAppWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.trim().length < 3) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(350) // Debounce autocompletion query
            try {
                val results = repository.searchLocations(newQuery)
                _searchResults.value = results
            } catch (e: Exception) {
                // Log or fail gracefully, do not crash autocompletion
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = _currentLocation.value
            val isFav = isCurrentFavorite.value
            if (isFav) {
                repository.deleteFavoriteByCoordinates(current.latitude, current.longitude)
            } else {
                repository.insertFavorite(
                    FavoriteLocation(
                        name = current.name,
                        latitude = current.latitude,
                        longitude = current.longitude,
                        country = current.country,
                        admin1 = current.admin1
                    )
                )
            }
        }
    }

    fun toggleTemperatureUnit() {
        _isCelsius.value = !_isCelsius.value
    }

    @SuppressLint("MissingPermission")
    fun loadWeatherFromGps() {
        val context = getApplication<Application>().applicationContext
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        _weatherState.value = WeatherUiState.Loading
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch {
                        val lat = location.latitude
                        val lon = location.longitude
                        
                        var cityName = "Ubicación Actual"
                        var countryName = ""
                        var adminArea: String? = null
                        
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(lat, lon, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Ubicación Actual"
                                countryName = addr.countryName ?: ""
                                adminArea = addr.adminArea
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        selectLocation(
                            city = cityName,
                            country = countryName,
                            admin1 = adminArea,
                            lat = lat,
                            lon = lon
                        )
                    }
                } else {
                    tryLocationManagerFallback()
                }
            }
            .addOnFailureListener {
                tryLocationManagerFallback()
            }
    }

    private fun tryLocationManagerFallback() {
        val context = getApplication<Application>().applicationContext
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: android.location.Location? = null
            for (provider in providers) {
                @SuppressLint("MissingPermission")
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                viewModelScope.launch {
                    val lat = bestLocation.latitude
                    val lon = bestLocation.longitude
                    var cityName = "Ubicación Actual"
                    var countryName = ""
                    var adminArea: String? = null
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Ubicación"
                            countryName = addr.countryName ?: ""
                            adminArea = addr.adminArea
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    selectLocation(
                        city = cityName,
                        country = countryName,
                        admin1 = adminArea,
                        lat = lat,
                        lon = lon
                    )
                }
            } else {
                fetchWeatherForCurrentLocation()
            }
        } catch (e: Exception) {
            fetchWeatherForCurrentLocation()
        }
    }
}
