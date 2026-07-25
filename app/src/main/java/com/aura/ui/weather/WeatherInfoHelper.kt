package com.aura.ui.weather

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

object WeatherInfoHelper {

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Cielo Despejado"
            1 -> "Principalmente Despejado"
            2 -> "Parcialmente Nublado"
            3 -> "Cubierto / Nublado"
            45 -> "Niebla"
            48 -> "Niebla Escarchada"
            51 -> "Llovizna Ligera"
            53 -> "Llovizna Moderada"
            55 -> "Llovizna Intensa"
            56 -> "Llovizna Helada Ligera"
            57 -> "Llovizna Helada Densa"
            61 -> "Lluvia Débil"
            63 -> "Lluvia Moderada"
            65 -> "Lluvia Fuerte"
            66 -> "Lluvia Helada Ligera"
            67 -> "Lluvia Helada Fuerte"
            71 -> "Nevada Ligera"
            73 -> "Nevada Moderada"
            75 -> "Nevada Intensa"
            77 -> "Granos de Nieve"
            80 -> "Chubascos de Lluvia Débiles"
            81 -> "Chubascos de Lluvia Moderados"
            82 -> "Chubascos de Lluvia Violentos"
            85 -> "Chubascos de Nieve Débiles"
            86 -> "Chubascos de Nieve Fuertes"
            95 -> "Tormenta Eléctrica"
            96 -> "Tormenta con Granizo Ligero"
            99 -> "Tormenta con Granizo Fuerte"
            else -> "Condiciones Variables"
        }
    }

    // Dynamic Atmospheric Background Gradient tuned for global dark theme
    fun getWeatherGradient(code: Int, isDay: Boolean): Brush {
        val colors = if (isDay) {
            when (code) {
                0, 1 -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)) // Dark cyan day sky
                2, 3 -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569)) // Dark overcast slate
                45, 48 -> listOf(Color(0xFF1A202C), Color(0xFF2D3748), Color(0xFF4A5568)) // Dark foggy slate
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)) // Dark rain slate
                71, 73, 75, 77, 85, 86 -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569)) // Dark snow slate
                95, 96, 99 -> listOf(Color(0xFF0B0F19), Color(0xFF181E29), Color(0xFF2D3748)) // Storm dark slate
                else -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            }
        } else {
            // Night gradients
            when (code) {
                0, 1 -> listOf(Color(0xFF090D16), Color(0xFF0F172A), Color(0xFF1E293B)) // Deep starry night
                2, 3 -> listOf(Color(0xFF0B0F19), Color(0xFF151D30), Color(0xFF253047)) // Dark cloudy night
                45, 48 -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)) // Dark misty night
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> listOf(Color(0xFF080C14), Color(0xFF0F172A), Color(0xFF1E293B)) // Dark rain night
                71, 73, 75, 77, 85, 86 -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)) // Dark snow night
                95, 96, 99 -> listOf(Color(0xFF05080E), Color(0xFF0B0F19), Color(0xFF181E29)) // Dark storm night
                else -> listOf(Color(0xFF090D16), Color(0xFF0F172A), Color(0xFF1E293B))
            }
        }
        return Brush.verticalGradient(colors)
    }

    // Identify if there is an extreme alert
    fun getExtremeAlerts(temp: Double, uvIndex: Double, windSpeed: Double, precipProb: Int, code: Int): List<String> {
        val alerts = mutableListOf<String>()
        if (temp >= 35.0) {
            alerts.add("Calor Extremo: Temperatura superior a 35°C. Mantente hidratado y evita el sol directo.")
        }
        if (temp <= 0.0) {
            alerts.add("Frío Extremo: Riesgo de heladas importantes. Abrigarse adecuadamente.")
        }
        if (uvIndex >= 7.0) {
            alerts.add("UV Extremo (Índice $uvIndex): Radiación muy alta. Aplica protector solar FPS 50+.")
        }
        if (windSpeed >= 40.0) {
            alerts.add("Vientos Fuertes ($windSpeed km/h): Alerta de ráfagas importantes. Precaución al aire libre.")
        }
        if (code in listOf(95, 96, 99)) {
            alerts.add("Tormenta Eléctrica Detectada: Busca refugio seguro e interior, peligro de rayos.")
        } else if (code in listOf(65, 82)) {
            alerts.add("Precipitación Severa: Riesgo Screen por fuertes lluvias acumuladas.")
        }
        return alerts
    }

    fun getWidgetAlert(temp: Double, uvIndex: Double, windSpeed: Double, code: Int): Pair<String, Boolean>? {
        // Red alerts (extreme priority warning)
        if (temp >= 40.0) return Pair("🚨 Ola calor extrema: ${temp.toInt()}°C", true)
        if (temp <= -5.0) return Pair("🚨 Ola de frío extremo: ${temp.toInt()}°C", true)
        if (code == 99) return Pair("🚨 Tormenta con granizo destructivo", true)
        if (windSpeed >= 60.0) return Pair("⚠️ Viento destructivo: ${windSpeed.toInt()}km/h", true)
        if (uvIndex >= 10.0) return Pair("⚠️ Radiación UV extrema de $uvIndex", true)

        // Orange alerts (severe warning)
        if (temp >= 35.0) return Pair("⚠️ Calor severo, mantente hidratado", false)
        if (temp <= 0.0) return Pair("⚠️ Helada severa detectada: ${temp.toInt()}°C", false)
        if (code in listOf(95, 96)) return Pair("⚡ Peligro de tormentas eléctricas", false)
        if (code in listOf(65, 82)) return Pair("🌧️ Precipitación torrencial intensa", false)
        if (windSpeed >= 40.0) return Pair("🌬️ Ráfagas fuertes: ${windSpeed.toInt()} km/h", false)
        if (uvIndex >= 7.0) return Pair("☀️ UV Muy alto. Usa protector FPS 50+", false)

        return null
    }
}
