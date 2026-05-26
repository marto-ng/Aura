package com.example.ui.weather

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.DailyWeather
import com.example.data.api.GeocodingResult
import com.example.data.api.HourlyWeather
import com.example.data.api.WeatherResponse
import com.example.data.db.FavoriteLocation
import java_text_SimpleDateFormat_compatibility.getDayOfWeekSpanish

@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isCelsius by viewModel.isCelsius.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isCurrentFavorite by viewModel.isCurrentFavorite.collectAsState()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Set up location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.loadWeatherFromGps()
        }
    }

    // Attempt GPS auto-retrieval immediately on start if permission is already granted
    LaunchedEffect(Unit) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasFine || hasCoarse) {
            viewModel.loadWeatherFromGps()
        } else {
            // Prompt for permission automatically on startup
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Present localized time state
    val currentTimeState = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM • HH:mm", java.util.Locale("es", "ES"))
            val formatted = sdf.format(java.util.Date()).replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
            }
            currentTimeState.value = formatted
            kotlinx.coroutines.delay(10000)
        }
    }

    // Extract code and isDay to construct background gradient dynamically
    val currentInfo = (weatherState as? WeatherUiState.Success)?.weather?.current
    val weatherCode = currentInfo?.weatherCode ?: 0
    val isDay = currentInfo?.isDay == 1

    val backgroundBrush = WeatherInfoHelper.getWeatherGradient(weatherCode, isDay)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Search Bar with Autocomplete & GPS Action Button
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Buscar ciudad...", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = Color.White
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                    }),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_field_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (hasFine || hasCoarse) {
                            viewModel.loadWeatherFromGps()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("gps_locate_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Utilizar GPS y autoubicación"
                    )
                }
            }

                // Dropdown autocomplete search results sheet
                if (isSearching || searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("search_dropdown_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (isSearching) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buscando ubicaciones...", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                searchResults.take(6).forEach { result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectLocation(
                                                    city = result.name,
                                                    country = result.country,
                                                    admin1 = result.admin1,
                                                    lat = result.latitude,
                                                    lon = result.longitude
                                                )
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Place icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = result.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            val region = buildString {
                                                if (!result.admin1.isNullOrBlank()) {
                                                    append(result.admin1)
                                                    append(", ")
                                                }
                                                append(result.country)
                                            }
                                            Text(
                                                text = region,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Favorites Pills Horizontal Scroll Row
            Text(
                text = "Favoritos",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin favoritos. Presiona la estrella para agregarlos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(favorites) { fav ->
                        val isSelected = kotlin.math.abs(fav.latitude - currentLocation.latitude) < 0.05 &&
                                         kotlin.math.abs(fav.longitude - currentLocation.longitude) < 0.05
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.selectFavorite(fav) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fav.name,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Main Weather State Box
            when (val state = weatherState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Actualizando pronóstico...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                is WeatherUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error icon",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ocurrió un error",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchWeatherForCurrentLocation() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reintentar", color = Color.White)
                                }
                            }
                        }
                    }
                }
                is WeatherUiState.Success -> {
                    // Weather Dashboard Detail inside a Scrollable column
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            // Header Location view with star bookmark and C/F selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentLocation.name,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val regionLabel = buildString {
                                        if (!currentLocation.admin1.isNullOrBlank()) {
                                            append(currentLocation.admin1)
                                            append(", ")
                                        }
                                        append(currentLocation.country)
                                    }
                                    Text(
                                        text = regionLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentTimeState.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Star Toggle Favorite
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite() },
                                        modifier = Modifier.testTag("favorite_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Toggle favorite status",
                                            tint = if (isCurrentFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // C/F Toggle Switch Selector
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .clickable { viewModel.toggleTemperatureUnit() }
                                            .testTag("unit_toggle_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isCelsius) "°C" else "°F",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // UV / Wind / Temp alerts banner
                        val currentTemp = state.weather.current?.temperature2m ?: 0.0
                        val uvIndex = state.weather.current?.uvIndex ?: 0.0
                        val windSpeed = state.weather.current?.windSpeed10m ?: 0.0
                        val rainProb = state.weather.hourly?.precipitationProbability?.firstOrNull() ?: 0
                        val alerts = WeatherInfoHelper.getExtremeAlerts(
                            temp = currentTemp,
                            uvIndex = uvIndex,
                            windSpeed = windSpeed,
                            precipProb = rainProb,
                            code = weatherCode
                        )

                        if (alerts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFE53935).copy(alpha = 0.85f))
                                        .border(1.dp, Color(0xFFFF8A80), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning alert",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                "AVISOS IMPORTANTES METEOROLÓGICOS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            alerts.forEach { alert ->
                                                Text(
                                                    text = "• $alert",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Big Temperature Display with Custom Graphic Icon
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatTemp(currentTemp, isCelsius),
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Text(
                                        text = WeatherInfoHelper.getWeatherDescription(weatherCode),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Interactive Animated/Canvas graphic representing condition
                                WeatherConditionGraphic(
                                    code = weatherCode,
                                    isDay = isDay,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .weight(0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Hourly carousel for next 24 Hours
                        item {
                            Text(
                                text = "Próximas 24 Horas",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            state.weather.hourly?.let { hourly ->
                                val indices = remember(hourly.time) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
                                        val currentHourStr = sdf.format(java.util.Date())
                                        val startIdx = hourly.time.indexOfFirst { it.startsWith(currentHourStr.substring(0, 13)) }
                                        val startIndexVal = if (startIdx != -1) startIdx else 0
                                        (startIndexVal until (startIndexVal + 24).coerceAtMost(hourly.time.size)).toList()
                                    } catch (e: Exception) {
                                        (0..23).toList()
                                    }
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(indices) { index ->
                                        val timeStr = hourly.time.getOrNull(index) ?: ""
                                        val temp = hourly.temperature2m.getOrNull(index) ?: 0.0
                                        val prob = hourly.precipitationProbability.getOrNull(index) ?: 0
                                        val code = hourly.weatherCode.getOrNull(index) ?: 0

                                        val isHourlyDay = try {
                                            val hour = timeStr.substringAfter('T').substringBefore(':').toInt()
                                            hour in 6..18
                                        } catch (e: Exception) {
                                            true
                                        }

                                        Card(
                                            modifier = Modifier.width(76.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(vertical = 12.dp)
                                                    .fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = formatHour(timeStr),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                WeatherConditionGraphic(
                                                    code = code,
                                                    isDay = isHourlyDay,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = formatTemp(temp, isCelsius),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                if (prob > 0) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "☔ $prob%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF80DEEA),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // 7-Day Forecast Card
                        item {
                            Text(
                                text = "Pronóstico de 7 Días",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            state.weather.daily?.let { daily ->
                                val daysCount = daily.time.size
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        for (i in 0 until daysCount) {
                                            DailyForecastRow(
                                                date = daily.time[i],
                                                code = daily.weatherCode[i],
                                                tempMin = daily.temperature2mMin[i],
                                                tempMax = daily.temperature2mMax[i],
                                                prob = daily.precipitationProbabilityMax.getOrNull(i) ?: 0,
                                                isCelsius = isCelsius
                                            )
                                            if (i < daysCount - 1) {
                                                HorizontalDivider(
                                                    color = Color.White.copy(alpha = 0.1f),
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Detailed Meteorological asymmetric grid
                        item {
                            Text(
                                text = "Detalles Meteorológicos",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val dailyInfo = state.weather.daily
                            val feelsLike = currentInfo?.apparentTemperature ?: 0.0
                            val windDir = currentInfo?.windDirection10m ?: 0.0
                            val humidity = currentInfo?.relativeHumidity2m ?: 0.0
                            val sunriseTime = dailyInfo?.sunrise?.firstOrNull()?.let { formatHour(it) } ?: "--:--"
                            val sunsetTime = dailyInfo?.sunset?.firstOrNull()?.let { formatHour(it) } ?: "--:--"

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Apparent temperature Card (Asymmetric)
                                    MetricCard(
                                        title = "Sensación Térmica",
                                        value = formatTemp(feelsLike, isCelsius),
                                        icon = "🌡️",
                                        subtitle = "Clima aparente",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Wind Speed Card (Asymmetric)
                                    MetricCard(
                                        title = "Viento",
                                        value = "$windSpeed km/h",
                                        icon = "💨",
                                        subtitle = "Dirección: $windDir°",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Relative Humidity Card
                                    MetricCard(
                                        title = "Humedad Relativa",
                                        value = "${humidity.toInt()}%",
                                        icon = "💧",
                                        subtitle = "Humedad del aire",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Precipitation probability Card
                                    MetricCard(
                                        title = "Lluvia",
                                        value = "${rainProb}%",
                                        icon = "☔",
                                        subtitle = "Prob. de precipitación",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // UV Index Card
                                    MetricCard(
                                        title = "Índice UV",
                                        value = "$uvIndex",
                                        icon = "☀️",
                                        subtitle = when {
                                            uvIndex < 3.0 -> "Bajo"
                                            uvIndex < 6.0 -> "Moderado"
                                            uvIndex < 8.0 -> "Alto"
                                            else -> "Muy Alto / Extremo"
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Sunrise & Sunset Card
                                    MetricCard(
                                        title = "Sol (Ciclo Solar)",
                                        value = "☀️ $sunriseTime",
                                        icon = "🌅",
                                        subtitle = "Atardecer: $sunsetTime",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7-day row
@Composable
fun DailyForecastRow(
    date: String,
    code: Int,
    tempMin: Double,
    tempMax: Double,
    prob: Int,
    isCelsius: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = java_text_SimpleDateFormat_compatibility.getDayOfWeekSpanish(date),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            // format date neatly
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WeatherConditionGraphic(
                code = code,
                isDay = true, // default icons for days to day style
                modifier = Modifier.size(28.dp)
            )
            if (prob > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$prob%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF80DEEA),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTemp(tempMax, isCelsius),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatTemp(tempMin, isCelsius),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// Compact metric Card styling
@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = icon, fontSize = 18.sp)
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Dynamic Minimalist Weather Drawing using standard Jetpack Compose Canvas
@Composable
fun WeatherConditionGraphic(
    code: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val sizeClass = kotlin.math.min(w, h)

        when (code) {
            0, 1 -> { // Clear / Mainly clear
                if (isDay) {
                    // Draw elegant sun
                    val r = sizeClass * 0.28f
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                    // radiating rays
                    val spokeCount = 8
                    val spokeLength = sizeClass * 0.12f
                    val spokeInner = r + 4f
                    for (i in 0 until spokeCount) {
                        val angle = (i * 2 * Math.PI / spokeCount).toFloat()
                        val startX = cx + spokeInner * kotlin.math.cos(angle)
                        val startY = cy + spokeInner * kotlin.math.sin(angle)
                        val endX = cx + (spokeInner + spokeLength) * kotlin.math.cos(angle)
                        val endY = cy + (spokeInner + spokeLength) * kotlin.math.sin(angle)
                        drawLine(
                            color = Color(0xFFFFB300),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                } else {
                    // Draw crescent moon for night
                    val r = sizeClass * 0.25f
                    val moonPath = androidx.compose.ui.graphics.Path().apply {
                        val moonCenter = Offset(cx - sizeClass * 0.05f, cy)
                        addOval(androidx.compose.ui.geometry.Rect(moonCenter, r))
                    }
                    val subtractPath = androidx.compose.ui.graphics.Path().apply {
                        val cutCenter = Offset(cx + sizeClass * 0.08f, cy - sizeClass * 0.05f)
                        addOval(androidx.compose.ui.geometry.Rect(cutCenter, r * 1.05f))
                    }
                    val finalMoonPath = androidx.compose.ui.graphics.Path.combine(
                        androidx.compose.ui.graphics.PathOperation.Difference,
                        moonPath,
                        subtractPath
                    )
                    drawPath(finalMoonPath, Color(0xFFECEFF1))
                    
                    // Draw some twinkling stars
                    drawCircle(Color.White, 3f, Offset(cx + sizeClass * 0.24f, cy - sizeClass * 0.24f))
                    drawCircle(Color.White.copy(alpha = 0.7f), 2f, Offset(cx - sizeClass * 0.22f, cy - sizeClass * 0.15f))
                    drawCircle(Color.White.copy(alpha = 0.8f), 3f, Offset(cx + sizeClass * 0.12f, cy + sizeClass * 0.26f))
                }
            }
            2, 3 -> { // Partly Cloudy / Overcast
                // Draw soft fluffy cloud base circles
                drawCircle(Color.White.copy(alpha = 0.85f), sizeClass * 0.2f, Offset(cx - sizeClass * 0.15f, cy + sizeClass * 0.05f))
                drawCircle(Color.White, sizeClass * 0.26f, Offset(cx, cy - sizeClass * 0.08f))
                drawCircle(Color.White.copy(alpha = 0.9f), sizeClass * 0.18f, Offset(cx + sizeClass * 0.16f, cy + sizeClass * 0.05f))
                // base bar to connect them
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx - sizeClass * 0.22f, cy - sizeClass * 0.03f),
                    size = androidx.compose.ui.geometry.Size(sizeClass * 0.45f, sizeClass * 0.16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(sizeClass * 0.08f, sizeClass * 0.08f)
                )
            }
            45, 48 -> { // Fog
                // Horizontal layered wave lines
                val barW = sizeClass * 0.5f
                val barH = sizeClass * 0.08f
                drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(cx - barW/2, cy - sizeClass*0.15f), androidx.compose.ui.geometry.Size(barW, barH), androidx.compose.ui.geometry.CornerRadius(barH/2))
                drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(cx - barW/2 - 10f, cy), androidx.compose.ui.geometry.Size(barW + 20f, barH), androidx.compose.ui.geometry.CornerRadius(barH/2))
                drawRoundRect(Color.White.copy(alpha = 0.4f), Offset(cx - barW/2 + 5f, cy + sizeClass*0.15f), androidx.compose.ui.geometry.Size(barW - 10, barH), androidx.compose.ui.geometry.CornerRadius(barH/2))
            }
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> { // Rain / Showers
                // Cloud
                drawCircle(Color(0xFFCFD8DC), sizeClass * 0.18f, Offset(cx - sizeClass * 0.12f, cy - sizeClass * 0.02f))
                drawCircle(Color(0xFFECEFF1), sizeClass * 0.22f, Offset(cx + sizeClass * 0.02f, cy - sizeClass * 0.1f))
                // Rain slant drops
                val dropW = 2.dp.toPx()
                val dropH = 8.dp.toPx()
                drawLine(Color(0xFF80DEEA), Offset(cx - sizeClass*0.1f, cy + sizeClass*0.1f), Offset(cx - sizeClass*0.15f, cy + sizeClass*0.22f), strokeWidth = dropW)
                drawLine(Color(0xFF80DEEA), Offset(cx, cy + sizeClass*0.12f), Offset(cx - sizeClass*0.05f, cy + sizeClass*0.24f), strokeWidth = dropW)
                drawLine(Color(0xFF80DEEA), Offset(cx + sizeClass*0.1f, cy + sizeClass*0.12f), Offset(cx + sizeClass*0.05f, cy + sizeClass*0.24f), strokeWidth = dropW)
            }
            71, 73, 75, 77, 85, 86 -> { // Snow
                // Soft Cloud
                drawCircle(Color.White, sizeClass * 0.18f, Offset(cx - sizeClass * 0.1f, cy - sizeClass * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.9f), sizeClass * 0.22f, Offset(cx + sizeClass * 0.05f, cy - sizeClass * 0.12f))
                // Snow flakes as circles
                drawCircle(Color.White, 3.dp.toPx(), Offset(cx - sizeClass*0.08f, cy + sizeClass * 0.12f))
                drawCircle(Color.White, 4.dp.toPx(), Offset(cx + sizeClass*0.05f, cy + sizeClass * 0.15f))
                drawCircle(Color.White, 3.dp.toPx(), Offset(cx - sizeClass*0.02f, cy + sizeClass * 0.22f))
            }
            95, 96, 99 -> { // Thunderstorm
                // Intense cloud
                drawCircle(Color(0xFF455A64), sizeClass * 0.2f, Offset(cx - sizeClass * 0.12f, cy - sizeClass * 0.05f))
                drawCircle(Color(0xFF37474F), sizeClass * 0.24f, Offset(cx + sizeClass * 0.05f, cy - sizeClass * 0.12f))
                // Lightning bolt lines
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy + sizeClass * 0.02f)
                    lineTo(cx - sizeClass * 0.08f, cy + sizeClass * 0.14f)
                    lineTo(cx + sizeClass * 0.02f, cy + sizeClass * 0.14f)
                    lineTo(cx - sizeClass * 0.04f, cy + sizeClass * 0.28f)
                }
                drawPath(path, Color(0xFFFFEB3B), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
            }
            else -> { // Default variable/unknown (Mix of sun/moon and cloud)
                if (isDay) {
                    drawCircle(Color(0xFFFFD54F), sizeClass * 0.18f, Offset(cx + sizeClass * 0.12f, cy - sizeClass * 0.12f))
                } else {
                    // moon behind cloud
                    val mr = sizeClass * 0.16f
                    val moonPath = androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(Offset(cx + sizeClass * 0.1f, cy - sizeClass * 0.1f), mr))
                    }
                    val subtractPath = androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(Offset(cx + sizeClass * 0.18f, cy - sizeClass * 0.13f), mr * 1.05f))
                    }
                    val finalMoonPath = androidx.compose.ui.graphics.Path.combine(
                        androidx.compose.ui.graphics.PathOperation.Difference,
                        moonPath,
                        subtractPath
                    )
                    drawPath(finalMoonPath, Color(0xFFECEFF1))
                }
                drawCircle(Color.White, sizeClass * 0.18f, Offset(cx - sizeClass * 0.08f, cy + sizeClass * 0.05f))
                drawCircle(Color.White, sizeClass * 0.22f, Offset(cx + sizeClass * 0.04f, cy - sizeClass * 0.02f))
            }
        }
    }
}

// Helpers
private fun formatTemp(celsius: Double, isCelsius: Boolean): String {
    val temp = if (isCelsius) celsius else (celsius * 9 / 5) + 32
    return "${temp.toInt()}°"
}

private fun formatHour(timeString: String): String {
    return try {
        val parts = timeString.split("T")
        if (parts.size == 2) {
            val hourMin = parts[1] // "14:00"
            // Let's just return the hour segment
            hourMin
        } else {
            timeString
        }
    } catch (e: Exception) {
        timeString
    }
}
