package com.macrodeck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrodeck.app.model.MarketItemConfig
import com.macrodeck.app.model.MarketQuote
import com.macrodeck.app.network.MarketRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MarketScreen(
    items: List<MarketItemConfig>,
    onBack: () -> Unit
) {
    var quotes by remember(items) { mutableStateOf<List<MarketQuote>>(emptyList()) }
    var selectedSingleQuoteId by remember(items) { mutableStateOf<String?>(null) }
    var lastUpdatedText by remember { mutableStateOf("") }
    var isLoading by remember(items) { mutableStateOf(true) }

    val activeItems = if (items.isNotEmpty()) items else listOf(
        MarketItemConfig(id = "BTCUSDT", name = "Bitcoin", category = "crypto", symbol = "BTCUSDT", color = "#38BDF8"),
        MarketItemConfig(id = "ETHUSDT", name = "Ethereum", category = "crypto", symbol = "ETHUSDT", color = "#10B981"),
        MarketItemConfig(id = "XU100.IS", name = "BIST 100", category = "bist", symbol = "XU100.IS", color = "#F59E0B"),
        MarketItemConfig(id = "GC=F", name = "Altın (ONS)", category = "gold", symbol = "GC=F", color = "#F43F5E")
    )

    // Canlı Polling Döngüsü (Sadece bu ekran açıkken 5 saniyede bir çalışır)
    LaunchedEffect(activeItems) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (isActive) {
            val result = MarketRepository.fetchQuotes(activeItems)
            quotes = result
            lastUpdatedText = sdf.format(Date())
            isLoading = false
            delay(5000)
        }
    }

    val isViewingDetail = (selectedSingleQuoteId != null && quotes.size > 1)

    // Telefonun sistem geri tuşunu yakala
    BackHandler {
        if (isViewingDetail) {
            selectedSingleQuoteId = null // Detay grafikten çoklu listeye dön
        } else {
            onBack() // Çoklu listeden veya tekil moddan ana menüye dön
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==================== 1: ÜST BAŞLIK & GERİ BUTONU ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Geri Dön Butonu (Tekil detaya girilmişse çoklu listeye döner, aksi halde ana menüye döner)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (isViewingDetail) {
                                selectedSingleQuoteId = null // Listeye dön
                            } else {
                                onBack() // Ana menüye dön
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isViewingDetail) "Listeye Dön" else "Ana Menü",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Sağ: Canlı Durum, İpucu ve Saat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isViewingDetail && quotes.size > 1) {
                        Text(
                            text = "💡 Grafiği için dokunun",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF22C55E), shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoading) "Veriler Alınıyor..." else "Canlı ($lastUpdatedText)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BatteryIndicator()
                }
            }

            // ==================== 2: GÖVDE (TEKİL GRAFİK VEYA ÇOKLU SATIRLAR) ====================
            if (quotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B0F19)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Piyasa verileri yükleniyor...", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else if (quotes.size == 1) {
                // TEKİL MOD: Büyük Detaylı Grafik & Canlı Kart
                SingleAssetChart(quote = quotes[0])
            } else {
                // ÇOKLU MOD:
                val detailedQuote = if (selectedSingleQuoteId != null) quotes.find { it.id == selectedSingleQuoteId } else null

                if (detailedQuote != null) {
                    // Kullanıcı çoklu listeden bir satıra tıkladığında o varlığın detay grafiğini göster
                    SingleAssetChart(quote = detailedQuote)
                } else {
                    // Yatay ekranda 2 sütun (sol & sağ) olarak dağıtılan satır satır liste (10 taneye kadar)
                    val half = (quotes.size + 1) / 2
                    val leftList = quotes.take(half)
                    val rightList = quotes.drop(half)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Sol Sütun
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            leftList.forEach { q ->
                                AssetRowItem(quote = q, onClick = { selectedSingleQuoteId = q.id })
                            }
                        }

                        // Sağ Sütun
                        if (rightList.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rightList.forEach { q ->
                                    AssetRowItem(quote = q, onClick = { selectedSingleQuoteId = q.id })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tekil Seçim Modu: Detaylı grafikli görünüm.
 * Fiyat güncellendiğinde önceki değere göre grafik arkasında 0.75 sn süren hafif yeşil/kırmızı efekt oluşturur.
 */
@Composable
fun SingleAssetChart(quote: MarketQuote) {
    var prevPrice by remember { mutableStateOf(quote.price) }
    var flashDirection by remember { mutableStateOf(0) } // 1: artış (yeşil), -1: azalış (kırmızı), 0: nötr

    LaunchedEffect(quote.price) {
        if (prevPrice > 0.0 && quote.price != prevPrice) {
            flashDirection = if (quote.price > prevPrice) 1 else -1
            prevPrice = quote.price
            delay(1000) // 1 saniye sürsün
            flashDirection = 0 // sonra default renge dönsün
        } else {
            prevPrice = quote.price
        }
    }

    val targetBgColor = when (flashDirection) {
        1 -> Color(0xFF14532D).copy(alpha = 0.5f) // Hafif yeşil
        -1 -> Color(0xFF7F1D1D).copy(alpha = 0.5f) // Hafif kırmızı
        else -> Color(0xFF0B0F19) // Default zemin
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = if (flashDirection != 0) 100 else 900),
        label = "singleChartBg"
    )

    val isDailyPositive = quote.changePercent >= 0
    val dailyColor = if (isDailyPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
    val lineColor = parseHexColor(quote.color)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .border(
                1.dp,
                if (flashDirection == 1) Color(0xFF22C55E) else if (flashDirection == -1) Color(0xFFEF4444) else Color(0xFF1E293B),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        // Üst Detay Bilgi & Günlük Değişim
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${quote.name} (${quote.symbol})",
                    color = lineColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPrice(quote.price, quote.category),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Günlük % Değişim Rozeti (eksi ise kırmızı, artı ise yeşil)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDailyPositive) Color(0xFF14532D) else Color(0xFF7F1D1D))
                    .border(1.dp, dailyColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${if (isDailyPositive) "▲ +" else "▼ "}${String.format(Locale.US, "%.2f", quote.changePercent)}%",
                    color = dailyColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grafik Çizimi (Canvas + Degrade Dolgu)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val pts = quote.history
            if (pts.size < 2) return@Canvas

            val minVal = pts.minOrNull() ?: 0.0
            val maxVal = pts.maxOrNull() ?: 1.0
            val range = (maxVal - minVal).coerceAtLeast(0.0001)

            val stepX = size.width / (pts.size - 1)
            val points = pts.mapIndexed { i, v ->
                val x = i * stepX
                val normalizedY = 1f - ((v - minVal) / range).toFloat()
                val y = (normalizedY * (size.height - 20f)) + 10f
                Offset(x, y)
            }

            // Çizgi Altı Degrade Alan
            val fillPath = Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = size.height
                )
            )

            // Ana Çizgi
            val linePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Son Nokta Vurgusu
            val lastPt = points.last()
            drawCircle(color = lineColor, radius = 5.dp.toPx(), center = lastPt)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = lastPt)
        }
    }
}

/**
 * Çoklu Seçim Modu: Tekil Satır Bileşeni.
 * Her güncellemede önceki değer ile kıyaslar:
 * Artış varsa -> 0.75 sn hafif yeşil
 * Azalış varsa -> 0.75 sn hafif kırmızı
 * Sabitse -> default renk
 */
@Composable
fun AssetRowItem(
    quote: MarketQuote,
    onClick: () -> Unit = {}
) {
    var prevPrice by remember { mutableStateOf(quote.price) }
    var flashDirection by remember { mutableStateOf(0) } // 1: artış, -1: azalış, 0: nötr

    LaunchedEffect(quote.price) {
        if (prevPrice > 0.0 && quote.price != prevPrice) {
            flashDirection = if (quote.price > prevPrice) 1 else -1
            prevPrice = quote.price
            delay(1000) // 1 saniye sürsün
            flashDirection = 0 // sonra default renge dönsün
        } else {
            prevPrice = quote.price
        }
    }

    val targetBgColor = when (flashDirection) {
        1 -> Color(0xFF14532D).copy(alpha = 0.6f) // Hafif yeşil zemin
        -1 -> Color(0xFF7F1D1D).copy(alpha = 0.6f) // Hafif kırmızı zemin
        else -> Color(0xFF0F1420) // Varsayılan zemin
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = if (flashDirection != 0) 100 else 900),
        label = "rowFlashBg"
    )

    val isDailyPositive = quote.changePercent >= 0
    val dailyColor = if (isDailyPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
    val accentColor = parseHexColor(quote.color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(animatedBg)
            .border(
                width = 1.dp,
                color = if (flashDirection == 1) Color(0xFF22C55E) else if (flashDirection == -1) Color(0xFFEF4444) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Sol: Renk çentiği & Varlık adı / sembolü
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.3f)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(30.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = quote.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = quote.symbol,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Orta: Canlı Fiyat
        Text(
            text = formatPrice(quote.price, quote.category),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Sağ: Günlük Değişim Yüzdesi (- yönde kırmızı, + yönde yeşil)
        Box(
            modifier = Modifier
                .width(76.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDailyPositive) Color(0xFF14532D).copy(alpha = 0.4f) else Color(0xFF7F1D1D).copy(alpha = 0.4f))
                .border(1.dp, dailyColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${if (isDailyPositive) "+" else ""}${String.format(Locale.US, "%.2f", quote.changePercent)}%",
                color = dailyColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Detay Grafiğe Git Oku
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Detay Grafik",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
    }
}

fun formatPrice(price: Double, category: String): String {
    return when {
        category == "crypto" -> "$${String.format(Locale.US, "%,.2f", price)}"
        category == "bist" -> "₺${String.format(Locale.US, "%,.2f", price)}"
        category == "gold" -> "$${String.format(Locale.US, "%,.1f", price)}"
        else -> String.format(Locale.US, "%,.2f", price)
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color((0xFF000000 or colorInt).toInt())
        } else {
            Color(colorInt.toInt())
        }
    } catch (_: Exception) {
        Color(0xFF38BDF8)
    }
}
