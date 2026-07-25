package com.aura.ui.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.widget.RemoteViews
import com.aura.MainActivity
import com.aura.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
        val cityName = prefs.getString("city_name", "Madrid") ?: "Madrid"
        val temp = prefs.getFloat("temperature", 22f)
        val weatherCode = prefs.getInt("weather_code", 0)
        val rainProbability = prefs.getInt("rain_probability", 15)
        val windSpeed = prefs.getFloat("wind_speed", 0f)
        val uvIndex = prefs.getFloat("uv_index", 0f)
        val trendStr = prefs.getString("temp_trend", "") ?: ""
        
        val weatherStatus = WeatherInfoHelper.getWeatherDescription(weatherCode)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Map weatherCode to personalized recommendation titles and subtitles for dynamic widget animation
        val (recommendation, recommendationSub) = when (weatherCode) {
            0, 1 -> Pair("☀️ ¡Cielo despejado!", "Buen clima para caminar ✨")
            2, 3 -> Pair("☁️ Se asoman nubes...", "Un cielo texturizado 🎨")
            45, 48 -> Pair("🌫️ Día con niebla", "Mucha precaución al andar ⚠️")
            in 51..57, in 61..67, in 80..82 -> Pair("🌧️ ¡Se aproxima lluvia!", "¡No olvides tu paraguas! ☔")
            in 71..77, in 85..86 -> Pair("❄️ ¡Está nevando!", "Abrígate muy bien hoy 🧥")
            95, 96, 99 -> Pair("⚡ Tormenta eléctrica", "Permanece hoy bajo techo 🏠")
            else -> Pair("🌤️ Clima cambiante", "Aprovecha al máximo tu día 🌟")
        }

        // Detect any active extreme weather alert using our WeatherInfoHelper
        val activeAlert = WeatherInfoHelper.getWidgetAlert(
            temp.toDouble(),
            uvIndex.toDouble(),
            windSpeed.toDouble(),
            weatherCode
        )

        // Parse or generate 24h temperature trend
        val trendList = if (trendStr.isNotEmpty()) {
            try {
                trendStr.split(",").map { it.toFloat() }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val finalTrend = if (trendList.size >= 2) {
            trendList
        } else {
            // Generate elegant daily temperature wave if no cache is available yet
            List(24) { i ->
                val wave = kotlin.math.sin(i * Math.PI / 12).toFloat()
                temp + wave * 4f
            }
        }

        val graphBitmap = drawTrendGraph(context, finalTrend)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            
            views.setTextViewText(R.id.widget_city, cityName)
            views.setTextViewText(R.id.widget_temp, "${temp.toInt()}°C")
            views.setTextViewText(R.id.widget_status, weatherStatus)
            views.setTextViewText(R.id.widget_rain, "💧 Lluvia: $rainProbability%")
            views.setTextViewText(R.id.widget_time, timeStr)
            
            // Set animation recommendation values for Slide 2 of ViewFlipper
            views.setTextViewText(R.id.widget_recommendation, recommendation)
            views.setTextViewText(R.id.widget_recommendation_sub, recommendationSub)

            // Setup dynamic extreme alerts banner if active
            if (activeAlert != null) {
                val (alertMessage, isRed) = activeAlert
                views.setViewVisibility(R.id.widget_alert_banner, View.VISIBLE)
                views.setTextViewText(R.id.widget_alert_text, alertMessage)
                
                // Color and backgrounds based on warning severity level (Red vs Orange)
                if (isRed) {
                    views.setTextColor(R.id.widget_alert_text, Color.parseColor("#EF4444"))
                    views.setInt(R.id.widget_alert_banner, "setBackgroundResource", R.drawable.bg_widget_alert_red)
                } else {
                    views.setTextColor(R.id.widget_alert_text, Color.parseColor("#FF9800"))
                    views.setInt(R.id.widget_alert_banner, "setBackgroundResource", R.drawable.bg_widget_alert_orange)
                }
            } else {
                views.setViewVisibility(R.id.widget_alert_banner, View.GONE)
            }

            // Assign the programmatic 24h temperature trend chart Bitmap
            if (graphBitmap != null) {
                views.setImageViewBitmap(R.id.widget_chart_image, graphBitmap)
            }
            
            // Pending Intent to launch main activity onClick
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun drawTrendGraph(context: Context, temps: List<Float>): Bitmap? {
        return try {
            val density = context.resources.displayMetrics.density
            val width = (320 * density).toInt()  // Width in pixels
            val height = (44 * density).toInt()  // Height in pixels
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val paintLine = Paint().apply {
                color = Color.parseColor("#00F2FE") // Cyan accent
                strokeWidth = 2f * density
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            
            val paintFill = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#4400F2FE"), // Semi-transparent cyan
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }

            val paintDot = Paint().apply {
                color = Color.parseColor("#00F2FE")
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val paintTextMinMax = Paint().apply {
                color = Color.WHITE
                textSize = 9f * density
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            if (temps.isEmpty()) {
                return bitmap
            }

            val minTemp = temps.minOrNull() ?: 0f
            val maxTemp = temps.maxOrNull() ?: 30f
            val tempRange = maxTemp - minTemp
            val paddingY = 10f * density
            val usableHeight = height - 2 * paddingY
            val stepX = width.toFloat() / (temps.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()

            val points = temps.mapIndexed { index, temp ->
                val x = index * stepX
                val ratio = if (tempRange == 0f) 0.5f else (temp - minTemp) / tempRange
                val y = height - (paddingY + ratio * usableHeight)
                PointF(x, y)
            }

            points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                    fillPath.moveTo(point.x, height.toFloat())
                    fillPath.lineTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                    fillPath.lineTo(point.x, point.y)
                }
                if (index == temps.size - 1) {
                    fillPath.lineTo(point.x, height.toFloat())
                    fillPath.close()
                }
            }

            canvas.drawPath(fillPath, paintFill)
            canvas.drawPath(path, paintLine)

            // Draw small indicators for MIN and MAX
            val minIdx = temps.indexOfFirst { it == minTemp }
            val maxIdx = temps.indexOfFirst { it == maxTemp }
            
            if (minIdx != -1) {
                val p = points[minIdx]
                canvas.drawCircle(p.x, p.y, 2.5f * density, paintDot)
                val yOffset = if (minIdx == maxIdx) -4f * density else 10f * density
                canvas.drawText("${minTemp.toInt()}°", p.x, (p.y + yOffset).coerceIn(10f * density, height - 1f), paintTextMinMax)
            }
            if (maxIdx != -1 && maxIdx != minIdx) {
                val p = points[maxIdx]
                canvas.drawCircle(p.x, p.y, 2.5f * density, paintDot)
                canvas.drawText("${maxTemp.toInt()}°", p.x, (p.y - 4f * density).coerceIn(10f * density, height - 1f), paintTextMinMax)
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // If updating or specific broadcast is requested
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WeatherAppWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isNotEmpty()) {
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
