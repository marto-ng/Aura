package com.aura.ui.weather

object WeatherStrings {
    // Search & Top Bar
    fun searchPlaceholder(isEnglish: Boolean) = if (isEnglish) "Search city..." else "Buscar ciudad..."
    fun searchingLocations(isEnglish: Boolean) = if (isEnglish) "Searching locations..." else "Buscando ubicaciones..."
    fun searchError(isEnglish: Boolean) = if (isEnglish) "Error searching locations" else "Error al buscar ubicaciones"
    fun noCitiesFound(query: String, isEnglish: Boolean) =
        if (isEnglish) "No cities found matching \"$query\"" else "No se encontraron ciudades con \"$query\""
    fun gpsLocateDescription(isEnglish: Boolean) = if (isEnglish) "Use GPS & auto-location" else "Utilizar GPS y autoubicación"

    // Favorites
    fun favoritesTitle(isEnglish: Boolean) = if (isEnglish) "FAVORITE CITIES" else "CIUDADES FAVORITAS"
    fun favoritesLabel(isEnglish: Boolean) = if (isEnglish) "Favorites" else "Favoritos"
    fun noFavorites(isEnglish: Boolean) = if (isEnglish) "No favorites. Tap the star icon to save cities." else "Sin favoritos. Presiona la estrella para agregarlos."

    // Status / Loading / Error
    fun updatingForecast(isEnglish: Boolean) = if (isEnglish) "Updating weather forecast..." else "Actualizando pronóstico..."
    fun couldNotLoad(isEnglish: Boolean) = if (isEnglish) "Could not load weather" else "No se pudo cargar el clima"
    fun retry(isEnglish: Boolean) = if (isEnglish) "Retry" else "Reintentar"
    fun defaultCityMadrid(isEnglish: Boolean) = if (isEnglish) "Madrid" else "Madrid"

    // Time & Alerts Header
    fun localTimeLabel(isEnglish: Boolean) = if (isEnglish) "City local time" else "Hora local de la ubicación"
    fun weatherAlertsHeader(isEnglish: Boolean) = if (isEnglish) "IMPORTANT WEATHER ALERTS" else "AVISOS IMPORTANTES METEOROLÓGICOS"

    // Hourly Forecast Chart
    fun hourlyForecastTitle(isEnglish: Boolean) = if (isEnglish) "24-Hour Forecast" else "Pronóstico 24 Horas"
    fun tempTrendTitle(isEnglish: Boolean) = if (isEnglish) "Temperature Trend (24h)" else "Tendencia de Temperatura (24h)"
    fun dragToExplore(isEnglish: Boolean) = if (isEnglish) "👆 Drag to explore" else "👆 Arrastra para explorar"
    fun now(isEnglish: Boolean) = if (isEnglish) "Now" else "Ahora"

    // 7-Day Forecast
    fun dailyForecastTitle(isEnglish: Boolean) = if (isEnglish) "7-Day Forecast" else "Pronóstico de 7 Días"
    fun occurrenceProb(min: Int, max: Int, isEnglish: Boolean) =
        if (isEnglish) "$min% - $max% occurrence prob." else "$min% - $max% prob. ocurrencia"
    fun dayOccurrenceProbShort(prob: Int, isEnglish: Boolean) =
        if (isEnglish) "$prob% prob." else "$prob% prob."

    // Details / Metrics
    fun meteorologicalDetailsTitle(isEnglish: Boolean) = if (isEnglish) "Weather Details" else "Detalles Meteorológicos"
    fun feelsLike(isEnglish: Boolean) = if (isEnglish) "Feels Like" else "Sensación Térmica"
    fun feelsLikeSubtitle(isEnglish: Boolean) = if (isEnglish) "Apparent temperature" else "Clima aparente"
    fun wind(isEnglish: Boolean) = if (isEnglish) "Wind" else "Viento"
    fun windDirSubtitle(degrees: Double, cardinal: String, isEnglish: Boolean) =
        if (isEnglish) "Dir: ${degrees.toInt()}° ($cardinal)" else "Dir: ${degrees.toInt()}° ($cardinal)"
    fun relativeHumidity(isEnglish: Boolean) = if (isEnglish) "Relative Humidity" else "Humedad Relativa"
    fun humiditySubtitle(isEnglish: Boolean) = if (isEnglish) "Air moisture" else "Humedad del aire"
    fun rain(isEnglish: Boolean) = if (isEnglish) "Rain" else "Lluvia"
    fun currentRainProbSubtitle(isEnglish: Boolean) = if (isEnglish) "Current probability" else "Probabilidad actual"
    fun precipitation(isEnglish: Boolean) = if (isEnglish) "Precipitation" else "Precipitación"
    fun precipAccumulatedSubtitle(isEnglish: Boolean) = if (isEnglish) "Accumulated this hour" else "Acumulada esta hora"
    fun cloudCover(isEnglish: Boolean) = if (isEnglish) "Cloud Cover" else "Nubosidad"
    fun cloudCoverSubtitle(isEnglish: Boolean) = if (isEnglish) "Sky coverage" else "Cobertura de nubes"
    fun uvIndex(isEnglish: Boolean) = if (isEnglish) "UV Index" else "Índice UV"
    fun uvSubtitle(uv: Double, isEnglish: Boolean) = when {
        uv < 3.0 -> if (isEnglish) "Low" else "Bajo"
        uv < 6.0 -> if (isEnglish) "Moderate" else "Moderado"
        uv < 8.0 -> if (isEnglish) "High" else "Alto"
        else -> if (isEnglish) "Very High / Extreme" else "Muy Alto / Extremo"
    }
    fun sunCycle(isEnglish: Boolean) = if (isEnglish) "Sun (Solar Cycle)" else "Sol (Ciclo Solar)"
    fun sunsetSubtitle(time: String, isEnglish: Boolean) = if (isEnglish) "Sunset: $time" else "Atardecer: $time"

    // Notifications / WorkManager Card
    fun morningSummaryTitle(isEnglish: Boolean) = if (isEnglish) "Morning Summary (WorkManager)" else "Resumen Matutino (WorkManager)"
    fun scheduledLabel(isEnglish: Boolean) = if (isEnglish) "SCHEDULED ⏰ 07:30 AM" else "PROGRAMADO ⏰ 07:30 AM"
    fun workManagerDescription(cityName: String, isEnglish: Boolean) =
        if (isEnglish) "WorkManager will send an automatic daily notification with the weather summary for $cityName."
        else "WorkManager enviará una notificación automática cada mañana con el resumen climático de $cityName."
    fun testNotificationButton(triggered: Boolean, isEnglish: Boolean) = if (triggered) {
        if (isEnglish) "Notification sent in background!" else "¡Notificación enviada en segundo plano!"
    } else {
        if (isEnglish) "Test morning summary now" else "Probar resumen matutino ahora"
    }

    // Settings Modal
    fun settingsTitle(isEnglish: Boolean) = if (isEnglish) "Settings" else "Ajustes"
    fun defaultLanguageTitle(isEnglish: Boolean) = if (isEnglish) "Default Language" else "Idioma por Defecto"
    fun defaultLanguageSubtitle(isEnglish: Boolean) = if (isEnglish) "Select application interface language" else "Selecciona el idioma de la interfaz"
    fun spanishOption(isEnglish: Boolean) = if (isEnglish) "Spanish (Español)" else "Español (Spanish)"
    fun englishOption(isEnglish: Boolean) = if (isEnglish) "English (Inglés)" else "English (Inglés)"
    fun tempUnitTitle(isEnglish: Boolean) = if (isEnglish) "Temperature Unit" else "Unidad de Temperatura"
    fun tempUnitSubtitle(isEnglish: Boolean) = if (isEnglish) "Choose °C (Celsius) or °F (Fahrenheit)" else "Elige °C (Celsius) o °F (Fahrenheit)"
    fun close(isEnglish: Boolean) = if (isEnglish) "Close" else "Cerrar"
    fun save(isEnglish: Boolean) = if (isEnglish) "Save" else "Guardar"

    // User messages / Snackbars
    fun favoriteAdded(cityName: String, isEnglish: Boolean) =
        if (isEnglish) "⭐ $cityName saved to favorites" else "⭐ $cityName guardada en favoritos"
    fun favoriteRemoved(cityName: String, isEnglish: Boolean) =
        if (isEnglish) "$cityName removed from favorites" else "$cityName eliminada de favoritos"
    fun gpsUpdated(cityName: String, isEnglish: Boolean) =
        if (isEnglish) "📍 Location updated: $cityName" else "📍 Ubicación actualizada: $cityName"
    fun gpsUnavailable(isEnglish: Boolean) =
        if (isEnglish) "GPS location unavailable. Showing default city." else "Ubicación GPS no disponible. Mostrando ciudad predeterminada."
    fun gpsError(isEnglish: Boolean) =
        if (isEnglish) "Error obtaining GPS location." else "Error al obtener la ubicación GPS."
    fun languageChangedMessage(isEnglish: Boolean) =
        if (isEnglish) "Language set to English" else "Idioma cambiado a Español"
}
