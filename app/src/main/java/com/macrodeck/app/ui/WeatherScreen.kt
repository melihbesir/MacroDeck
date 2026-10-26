package com.macrodeck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrodeck.app.model.DayForecast
import com.macrodeck.app.model.HourlyForecast
import com.macrodeck.app.model.WeatherData
import java.util.Calendar

enum class WeatherTab(val title: String) {
    TEMPERATURE("Sıcaklık"),
    PRECIPITATION("Yağış"),
    WIND("Rüzgar")
}

@Composable
fun WeatherScreen(
    weather: WeatherData,
    onBack: () -> Unit
) {
    var selectedDayIndex by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(WeatherTab.TEMPERATURE) }

    // Güvenlik kontrolü: Eğer days listesi boşsa varsayılan boş bir gün üret
    val activeDay: DayForecast = if (weather.days.isNotEmpty() && selectedDayIndex < weather.days.size) {
        weather.days[selectedDayIndex]
    } else {
        DayForecast(
            dayName = "Bugün",
            condition = weather.currentCondition,
            maxTemp = weather.currentTemp,
            minTemp = weather.currentTemp - 4
        )
    }

    // Telefonun sistem geri tuşunu yakala ve ana ekrana dön
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==================== 1: ÜST BAŞLIK VE GERİ BUTONU ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Geri Dön Butonu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ana Menü",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Sağ: Şehir ve Durum Bilgisi & Batarya
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${weather.cityName} Hava Durumu",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeDay.dayName} • ${activeDay.condition}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    BatteryIndicator()
                }
            }

            // ==================== 2: MEVCUT DURUM ÖZETİ & SEÇENEK SEKMELERİ ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol: Sıcaklık & İkon & Detaylar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getWeatherEmoji(activeDay.weatherCode),
                        fontSize = 38.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Büyük Sıcaklık Numarası
                    val displayTemp = if (selectedDayIndex == 0) weather.currentTemp else activeDay.maxTemp
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "$displayTemp",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " °C",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Nem, Yağış, Rüzgar
                    Column {
                        Text(
                            text = "Yağış: %${weather.precipitation}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Nem: %${weather.humidity}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Rüzgar: ${weather.windSpeed} km/s",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                }

                // Sağ: Google Tarzı Sekmeler (Sıcaklık | Yağış | Rüzgar)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeatherTab.values().forEach { tab ->
                        val isSelected = (selectedTab == tab)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(2.dp)
                                    .background(if (isSelected) Color(0xFFFBBF24) else Color.Transparent)
                            )
                        }
                    }
                }
            }

            // ==================== 3: İNTERAKTİF SAATLİK GRAFİK ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                HourlyWeatherChart(
                    hourlyList = activeDay.hourly,
                    tab = selectedTab,
                    isToday = (selectedDayIndex == 0)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==================== 4: ALT 7 GÜNLÜK FORECAST ŞERİDİ ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(weather.days) { index, day ->
                    val isSelected = (index == selectedDayIndex)
                    DayCard(
                        day = day,
                        isSelected = isSelected,
                        onClick = { selectedDayIndex = index }
                    )
                }
            }
        }
    }
}

@Composable
fun DayCard(
    day: DayForecast,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF141720))
            .border(
                1.dp,
                if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E222D),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.dayName,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = getWeatherEmoji(day.weatherCode),
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${day.maxTemp}° ${day.minTemp}°",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HourlyWeatherChart(
    hourlyList: List<HourlyForecast>,
    tab: WeatherTab,
    isToday: Boolean = true
) {
    if (hourlyList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Saatlik veri bekleniyor...", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Değer Etiketleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourlyList.forEach { h ->
                val label = when (tab) {
                    WeatherTab.TEMPERATURE -> "${h.temp}°"
                    WeatherTab.PRECIPITATION -> "%${h.precipitation}"
                    WeatherTab.WIND -> "${h.windSpeed}"
                }
                Text(
                    text = label,
                    color = when (tab) {
                        WeatherTab.TEMPERATURE -> Color(0xFFFBBF24)
                        WeatherTab.PRECIPITATION -> Color(0xFF38BDF8)
                        WeatherTab.WIND -> Color(0xFF34D399)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Grafik Çizimi (Canvas)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            val count = hourlyList.size
            if (count < 2) return@Canvas

            val values = hourlyList.map {
                when (tab) {
                    WeatherTab.TEMPERATURE -> it.temp.toFloat()
                    WeatherTab.PRECIPITATION -> it.precipitation.toFloat()
                    WeatherTab.WIND -> it.windSpeed.toFloat()
                }
            }

            val minVal = (values.minOrNull() ?: 0f).coerceAtMost(0f)
            val maxVal = (values.maxOrNull() ?: 100f).coerceAtLeast(minVal + 1f)
            val range = maxVal - minVal

            val stepX = size.width / (count - 1)
            val points = values.mapIndexed { i, v ->
                val x = i * stepX
                val normalizedY = 1f - ((v - minVal) / range)
                val y = (normalizedY * (size.height - 20f)) + 10f
                Offset(x, y)
            }

            val strokeColor = when (tab) {
                WeatherTab.TEMPERATURE -> Color(0xFFFBBF24)
                WeatherTab.PRECIPITATION -> Color(0xFF38BDF8)
                WeatherTab.WIND -> Color(0xFF34D399)
            }

            // Eğri çizgisi
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Noktalar
            points.forEach { pt ->
                drawCircle(color = strokeColor, radius = 3.5.dp.toPx(), center = pt)
                drawCircle(color = Color.Black, radius = 1.5.dp.toPx(), center = pt)
            }

            // ==================== MEVCUT SAAT İŞARETÇİSİ (YUKARIDAN AŞAĞI DİKEY ÇİZGİ) ====================
            if (isToday) {
                val now = Calendar.getInstance()
                val currentHour = now.get(Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(Calendar.MINUTE)
                val currentFloatHour = currentHour + (currentMinute / 60f)

                val hours = hourlyList.map {
                    it.time.substringBefore(":").toFloatOrNull() ?: 0f
                }

                if (hours.size >= 2) {
                    var currentX = points[0].x
                    var currentY = points[0].y

                    if (currentFloatHour <= hours.first()) {
                        currentX = points[0].x
                        currentY = points[0].y
                    } else if (currentFloatHour >= hours.last()) {
                        currentX = points.last().x
                        currentY = points.last().y
                    } else {
                        for (i in 0 until hours.size - 1) {
                            val h1 = hours[i]
                            val h2 = hours[i + 1]
                            if (currentFloatHour in h1..h2) {
                                val span = if (h2 > h1) h2 - h1 else 1f
                                val fraction = (currentFloatHour - h1) / span
                                currentX = points[i].x + fraction * (points[i + 1].x - points[i].x)
                                currentY = points[i].y + fraction * (points[i + 1].y - points[i].y)
                                break
                            }
                        }
                    }

                    // 1. Yukarıdan Aşağı Kesikli İşaretçi Çizgisi
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f)
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(currentX, 0f),
                        end = Offset(currentX, size.height),
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // 2. Eğri üzerindeki anlık konum noktası (Glow halo + iç beyaz nokta)
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 4.5.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )

                    // 3. Üstte 'Şimdi' Rozeti (Badge)
                    val badgeW = 32.dp.toPx()
                    val badgeH = 13.dp.toPx()
                    val badgeLeft = (currentX - badgeW / 2f).coerceIn(2f, size.width - badgeW - 2f)
                    val badgeRight = badgeLeft + badgeW
                    val badgeTop = 0f
                    val badgeBottom = badgeH

                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#0284C7")
                        isAntiAlias = true
                    }
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 8.5.sp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        badgeLeft,
                        badgeTop,
                        badgeRight,
                        badgeBottom,
                        3.5.dp.toPx(),
                        3.5.dp.toPx(),
                        bgPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "Şimdi",
                        badgeLeft + badgeW / 2f,
                        badgeBottom - 3.dp.toPx(),
                        textPaint
                    )
                }
            }
        }

        // Alt Saat Etiketleri (02:00, 05:00, ...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourlyList.forEach { h ->
                Text(
                    text = h.time,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun getWeatherEmoji(code: Int): String {
    return when (code) {
        0 -> "☀️"
        1, 2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65 -> "🌧️"
        71, 73, 75 -> "❄️"
        80, 81, 82 -> "⛈️"
        95, 96, 99 -> "🌩️"
        else -> "⛅"
    }
}
