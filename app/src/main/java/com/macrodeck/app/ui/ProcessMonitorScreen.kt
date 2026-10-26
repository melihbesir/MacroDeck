package com.macrodeck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrodeck.app.model.ProcessItemInfo
import com.macrodeck.app.model.ProcessStats

@Composable
fun ProcessMonitorScreen(
    processStats: ProcessStats,
    processList: List<ProcessItemInfo>,
    targetProcessName: String = "",
    isConnected: Boolean = true,
    onSelectProcess: (Int) -> Unit,
    onRefreshProcessList: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            onBack()
        }
    }

    var timeRangeMinutes by remember { mutableIntStateOf(1) } // 1, 5, 10 dk
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ProcessSelectionDialog(
            processList = processList,
            currentPid = processStats.pid,
            isAuto = processStats.isAutoActiveWindow,
            onSelectProcess = onSelectProcess,
            onDismiss = { showDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ==================== 1: ÜST KONTROL BAR VE BİLGİLER ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Geri Dön Butonu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .background(Color(0xFF141824))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ana Menü",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Orta: Takip Edilen Uygulama Seçim Hapı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141824))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .clickable {
                            onRefreshProcessList()
                            showDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (processStats.isAutoActiveWindow) "🎯 Aktif: " else "📌 Uygulama: ",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    val displayTitle = when {
                        processStats.title.isNotBlank() -> processStats.title
                        processStats.name.isNotBlank() -> processStats.name
                        targetProcessName.isNotBlank() -> targetProcessName
                        else -> "Uygulama Bekleniyor"
                    }
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                    if (processStats.pid > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "(${processStats.pid})",
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Değiştir ▾",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sağ: Zaman Aralığı Butonu ve Canlı Durum
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Zaman Aralığı Butonu (1 dk -> 5 dk -> 10 dk -> 1 dk)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                            .clickable {
                                timeRangeMinutes = when (timeRangeMinutes) {
                                    1 -> 5
                                    5 -> 10
                                    else -> 1
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "⏱️ ",
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$timeRangeMinutes dk",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Bağlantı Göstergesi (Yeşil / Kırmızı Nokta)
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )

                    Spacer(modifier = Modifier.width(10.dp))
                    BatteryIndicator()
                }
            }

            // Durum Uyarısı (Eğer hedef uygulama kapalıysa veya bekleniyorsa)
            if (processStats.pid == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF3B1D1D))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⏳ ${processStats.title.ifBlank { "Uygulamanın PC'de açılması bekleniyor..." }}",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ==================== 2: 5 KAYNAK KARTI DÜZENİ ====================
            // Üst Satır: CPU, RAM, GPU (3 kart)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProcessResourceCard(
                    title = "CPU Kullanımı",
                    icon = "⚡",
                    percent = processStats.cpuPercent,
                    extraText = "",
                    history = processStats.cpuHistory,
                    timeRangeMinutes = timeRangeMinutes,
                    primaryColor = Color(0xFFF97316),
                    gradientColors = listOf(Color(0xFFF97316), Color(0xFFFBBF24)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                ProcessResourceCard(
                    title = "RAM Bellek",
                    icon = "🧠",
                    percent = processStats.ramPercent,
                    extraText = "${processStats.ramMb.toInt()} MB",
                    history = processStats.ramHistory,
                    timeRangeMinutes = timeRangeMinutes,
                    primaryColor = Color(0xFFA855F7),
                    gradientColors = listOf(Color(0xFFA855F7), Color(0xFFEC4899)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                ProcessResourceCard(
                    title = "GPU Motoru",
                    icon = "🎮",
                    percent = processStats.gpuPercent,
                    extraText = "",
                    history = processStats.gpuHistory,
                    timeRangeMinutes = timeRangeMinutes,
                    primaryColor = Color(0xFF06B6D4),
                    gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF38BDF8)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // Alt Satır: Disk I/O, Ağ / Network (2 kart)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProcessResourceCard(
                    title = "Disk I/O Kullanımı",
                    icon = "💾",
                    percent = processStats.diskPercent,
                    extraText = "O: ${processStats.diskReadMb} MB/s | Y: ${processStats.diskWriteMb} MB/s",
                    history = processStats.diskHistory,
                    timeRangeMinutes = timeRangeMinutes,
                    primaryColor = Color(0xFF10B981),
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF14B8A6)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                ProcessResourceCard(
                    title = "Ağ (Network) Kullanımı",
                    icon = "🌐",
                    percent = processStats.netPercent,
                    extraText = "↓ ${processStats.netRecvMb} MB/s | ↑ ${processStats.netSendMb} MB/s",
                    history = processStats.netHistory,
                    timeRangeMinutes = timeRangeMinutes,
                    primaryColor = Color(0xFF6366F1),
                    gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

// ==================== TEK UYGULAMA KAYNAK KARTI ====================
@Composable
private fun ProcessResourceCard(
    title: String,
    icon: String,
    percent: Float,
    extraText: String,
    history: List<Float>,
    timeRangeMinutes: Int,
    primaryColor: Color,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF12141C))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Başlık, İkon ve Anlık Değer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (extraText.isNotBlank()) {
                        Text(
                            text = extraText,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "%${percent.toInt()}",
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 2. Soldan Sağa Yükselen Doluluk Barı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF242C3D))
            ) {
                val progressFraction = (percent / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(gradientColors))
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 3. Detaylı Geçmiş Grafiği (Sol Y-ekseni ve Alt Zaman Belirteçleri ile)
            DetailedProcessChart(
                history = history,
                timeRangeMinutes = timeRangeMinutes,
                lineColor = primaryColor,
                fillColor = primaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

// ==================== DETAYLI ÇOKLU ZAMAN GRAFİĞİ ====================
@Composable
private fun DetailedProcessChart(
    history: List<Float>,
    timeRangeMinutes: Int,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val totalSamples = when (timeRangeMinutes) {
        10 -> 600
        5 -> 300
        else -> 60
    }

    val visiblePoints = if (history.size > totalSamples) {
        history.takeLast(totalSamples)
    } else {
        List(totalSamples - history.size) { 0f } + history
    }

    val timeLabels = when (timeRangeMinutes) {
        10 -> listOf("-10 dk", "-8 dk", "-6 dk", "-4 dk", "-2 dk", "Şimdi")
        5 -> listOf("-5 dk", "-4 dk", "-3 dk", "-2 dk", "-1 dk", "Şimdi")
        else -> listOf("-60s", "-45s", "-30s", "-15s", "Şimdi")
    }

    val yLabels = listOf("100%", "75%", "50%", "25%", "0%")

    Row(modifier = modifier) {
        // Sol Y-Ekseni Belirteçleri (%100, %75, %50, %25, %0)
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .padding(bottom = 12.dp, top = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.forEach { label ->
                Text(
                    text = label,
                    color = Color(0xFF64748B),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Grafik Çizim Alanı ve Alt Zaman Belirteçleri
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Yatay Kılavuz Çizgileri (%100, %75, %50, %25, %0)
                    for (i in 0..4) {
                        val y = (h / 4f) * i
                        drawLine(
                            color = Color(0xFF1E2638),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (visiblePoints.isEmpty()) return@Canvas

                    val maxVal = 100f
                    val stepX = if (visiblePoints.size > 1) w / (visiblePoints.size - 1) else w

                    val strokePath = Path()
                    val fillPath = Path()

                    fillPath.moveTo(0f, h)

                    visiblePoints.forEachIndexed { i, value ->
                        val clamped = value.coerceIn(0f, maxVal)
                        val x = i * stepX
                        val y = (h - (clamped / maxVal * h)).coerceIn(1f, h - 1f)

                        if (i == 0) {
                            strokePath.moveTo(x, y)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevClamped = visiblePoints[i - 1].coerceIn(0f, maxVal)
                            val prevY = (h - (prevClamped / maxVal * h)).coerceIn(1f, h - 1f)
                            val cX = (prevX + x) / 2f
                            strokePath.cubicTo(cX, prevY, cX, y, x, y)
                            fillPath.cubicTo(cX, prevY, cX, y, x, y)
                        }
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(fillColor.copy(alpha = 0.35f), fillColor.copy(alpha = 0.03f)),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // En son nokta üzerine küçük gösterge daire
                    val lastX = (visiblePoints.size - 1) * stepX
                    val lastVal = visiblePoints.last().coerceIn(0f, maxVal)
                    val lastY = (h - (lastVal / maxVal * h)).coerceIn(2f, h - 2f)
                    drawCircle(color = lineColor, radius = 2.5.dp.toPx(), center = Offset(lastX, lastY))
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Alt Zaman Belirteçleri
            Row(
                modifier = Modifier.fillMaxWidth().height(11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeLabels.forEach { timeLabel ->
                    Text(
                        text = timeLabel,
                        color = Color(0xFF64748B),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== İŞLEM SEÇİM DİYALOĞU ====================
@Composable
private fun ProcessSelectionDialog(
    processList: List<ProcessItemInfo>,
    currentPid: Int,
    isAuto: Boolean,
    onSelectProcess: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .heightIn(max = 340.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF10131B))
                .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Takip Edilecek İşlemi Seçin",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kapat",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Otomatik Aktif Pencere Butonu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAuto) Color(0xFF1E3A8A) else Color(0xFF181C26))
                        .border(1.dp, if (isAuto) Color(0xFF3B82F6) else Color(0xFF263045), RoundedCornerShape(8.dp))
                        .clickable {
                            onSelectProcess(0)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🔄 Otomatik (Ön Plandaki Aktif Pencere)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PC'de hangi pencereye tıklarsanız onu anlık takip eder",
                                color = Color(0xFF94A3B8),
                                fontSize = 8.5.sp
                            )
                        }
                        if (isAuto) {
                            Text(text = "AKTİF", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Çalışan Uygulamalar (${processList.size})", color = Color(0xFF64748B), fontSize = 9.5.sp)
                Spacer(modifier = Modifier.height(4.dp))

                // Çalışan Uygulamalar Listesi
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(processList) { item ->
                        val isSelected = !isAuto && item.pid == currentPid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF141824))
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectProcess(item.pid)
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title.ifBlank { item.name },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.name}  •  PID: ${item.pid}",
                                    color = Color(0xFF64748B),
                                    fontSize = 8.sp
                                )
                            }
                            Text(
                                text = "${item.ramMb.toInt()} MB",
                                color = Color(0xFFA855F7),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
