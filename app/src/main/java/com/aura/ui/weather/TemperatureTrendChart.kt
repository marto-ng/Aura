package com.aura.ui.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.data.api.HourlyWeather

data class HourlyChartPoint(
    val index: Int,
    val rawHourString: String,
    val displayHour: String,
    val celsius: Double,
    val formattedTemp: String,
    val weatherCode: Int,
    val precipProbability: Int,
    val isDay: Boolean
)

@Composable
fun TemperatureTrendChart(
    hourly: HourlyWeather,
    indices: List<Int>,
    isCelsius: Boolean,
    isEnglish: Boolean = false,
    currentTemp: Double? = null,
    currentWeatherCode: Int? = null,
    modifier: Modifier = Modifier
) {
    if (indices.isEmpty()) return

    val chartData = remember(hourly, indices, isCelsius, isEnglish, currentTemp, currentWeatherCode) {
        indices.mapIndexed { i, idx ->
            val rawTime = hourly.time.getOrNull(idx) ?: ""
            val rawTempC = hourly.temperature2m.getOrNull(idx) ?: 0.0
            val rawCode = hourly.weatherCode.getOrNull(idx) ?: 0
            val prob = hourly.precipitationProbability.getOrNull(idx) ?: 0

            val tempC = if (i == 0 && currentTemp != null) currentTemp else rawTempC
            val code = if (i == 0 && currentWeatherCode != null) currentWeatherCode else rawCode
            
            val hourFormatted = if (i == 0) {
                WeatherStrings.now(isEnglish)
            } else {
                try {
                    val timePart = rawTime.substringAfter('T').substringBefore(':')
                    "$timePart:00"
                } catch (e: Exception) {
                    rawTime
                }
            }

            val isDaytime = try {
                val hourInt = rawTime.substringAfter('T').substringBefore(':').toInt()
                hourInt in 6..18
            } catch (e: Exception) {
                true
            }

            val tempVal = if (isCelsius) tempC else (tempC * 9 / 5) + 32

            HourlyChartPoint(
                index = i,
                rawHourString = rawTime,
                displayHour = hourFormatted,
                celsius = tempC,
                formattedTemp = "${tempVal.toInt()}°${if (isCelsius) "C" else "F"}",
                weatherCode = code,
                precipProbability = prob,
                isDay = isDaytime
            )
        }
    }

    // Currently selected point index from user touch/drag gesture
    var selectedIndex by remember { mutableStateOf<Int?>(0) }

    val minTempC = chartData.minOfOrNull { it.celsius } ?: 0.0
    val maxTempC = chartData.maxOfOrNull { it.celsius } ?: 30.0
    val rangeTemp = (maxTempC - minTempC).coerceAtLeast(2.0)

    val density = LocalDensity.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("temperature_trend_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Chart Header Title and Active Selection Tooltip Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F2FE))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = WeatherStrings.tempTrendTitle(isEnglish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = WeatherStrings.dragToExplore(isEnglish),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Touch Tooltip Display
            val activePoint = selectedIndex?.let { chartData.getOrNull(it) } ?: chartData.firstOrNull()
            if (activePoint != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeatherConditionGraphic(
                                code = activePoint.weatherCode,
                                isDay = activePoint.isDay,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activePoint.displayHour,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF00F2FE)
                                    )
                                    if (activePoint.precipProbability > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "💧 ${activePoint.precipProbability}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF80DEEA),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = WeatherInfoHelper.getWeatherDescription(activePoint.weatherCode, isEnglish),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Text(
                            text = activePoint.formattedTemp,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chartData) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (chartData.size - 1).coerceAtLeast(1)
                                val idx = (offset.x / stepX).toInt().coerceIn(0, chartData.size - 1)
                                selectedIndex = idx
                            }
                        }
                        .pointerInput(chartData) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val stepX = size.width / (chartData.size - 1).coerceAtLeast(1)
                                val idx = (change.position.x / stepX).toInt().coerceIn(0, chartData.size - 1)
                                selectedIndex = idx
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingTop = 28.dp.toPx()
                    val paddingBottom = 32.dp.toPx()
                    val usableHeight = height - paddingTop - paddingBottom
                    val stepX = width / (chartData.size - 1).coerceAtLeast(1)

                    // Draw Horizontal Grid Guide Lines
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val yRatio = i.toFloat() / gridLinesCount
                        val y = paddingTop + yRatio * usableHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // Calculate X and Y positions for each point
                    val points = chartData.mapIndexed { idx, pt ->
                        val x = idx * stepX
                        val ratio = ((pt.celsius - minTempC) / rangeTemp).toFloat()
                        val y = height - paddingBottom - (ratio * usableHeight)
                        Offset(x, y)
                    }

                    // Build Smooth Cubic Bezier Path
                    val linePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        linePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, height - paddingBottom)
                        fillPath.lineTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]

                            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)

                            linePath.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p2.x, p2.y
                            )

                            fillPath.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p2.x, p2.y
                            )
                        }

                        val lastPoint = points.last()
                        fillPath.lineTo(lastPoint.x, height - paddingBottom)
                        fillPath.close()

                        // Draw Gradient Fill Under Curve
                        val fillGradient = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00F2FE).copy(alpha = 0.35f),
                                Color(0xFF4FACFE).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                        drawPath(path = fillPath, brush = fillGradient)

                        // Draw Curve Line with Glow
                        drawPath(
                            path = linePath,
                            color = Color(0xFF00F2FE),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw X-Axis Hour Labels (Every 3-4 hours)
                    drawIntoCanvas { canvas ->
                        chartData.forEachIndexed { i, pt ->
                            if (i % 4 == 0 || i == chartData.size - 1) {
                                val ptOffset = points[i]
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.argb(180, 255, 255, 255)
                                    textSize = 10.sp.toPx()
                                    textAlign = when (i) {
                                        0 -> android.graphics.Paint.Align.LEFT
                                        chartData.size - 1 -> android.graphics.Paint.Align.RIGHT
                                        else -> android.graphics.Paint.Align.CENTER
                                    }
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                canvas.nativeCanvas.drawText(
                                    pt.displayHour,
                                    ptOffset.x.coerceIn(0f, width),
                                    height - 6.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }

                    // Highlight Active Selected Touch Point
                    val activeIdx = selectedIndex
                    if (activeIdx != null && activeIdx in points.indices) {
                        val activeOffset = points[activeIdx]

                        // Vertical touch line
                        drawLine(
                            color = Color(0xFF00F2FE).copy(alpha = 0.7f),
                            start = Offset(activeOffset.x, paddingTop - 10f),
                            end = Offset(activeOffset.x, height - paddingBottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Outer glowing pulse ring
                        drawCircle(
                            color = Color(0xFF00F2FE).copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = activeOffset
                        )

                        // Inner solid point
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = activeOffset
                        )

                        drawCircle(
                            color = Color(0xFF00F2FE),
                            radius = 4.dp.toPx(),
                            center = activeOffset
                        )
                    }

                    // Draw Min and Max Badges on the Curve
                    val minPtIdx = chartData.indexOfFirst { it.celsius == minTempC }
                    val maxPtIdx = chartData.indexOfFirst { it.celsius == maxTempC }

                    if (maxPtIdx != -1 && maxPtIdx in points.indices) {
                        val maxPos = points[maxPtIdx]
                        drawCircle(Color(0xFFFFD54F), 3.5.dp.toPx(), maxPos)
                    }

                    if (minPtIdx != -1 && minPtIdx != maxPtIdx && minPtIdx in points.indices) {
                        val minPos = points[minPtIdx]
                        drawCircle(Color(0xFF80DEEA), 3.5.dp.toPx(), minPos)
                    }
                }
            }
        }
    }
}
