package com.macrodeck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrodeck.app.model.CpuStats
import com.macrodeck.app.model.DiskItemStats
import com.macrodeck.app.model.FanItemStats
import com.macrodeck.app.model.GpuStats
import com.macrodeck.app.model.ProcessItemInfo
import com.macrodeck.app.model.ProcessStats
import com.macrodeck.app.model.RamStats
import com.macrodeck.app.model.SystemStats

@Composable
fun SystemMonitorScreen(
    stats: SystemStats,
    isConnected: Boolean = true,
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

    var selectedGpuIndex by remember { mutableStateOf(0) }
    val allGpus = if (stats.gpus.isNotEmpty()) stats.gpus else listOf(stats.gpu)
    val activeGpu = allGpus.getOrElse(selectedGpuIndex) { stats.gpu }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ==================== 1: ÜST BAŞLIK & GERİ BUTONU ====================
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

                // Orta: Başlık
                Text(
                    text = "🖥️ Donanım Sistem Monitörü",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Sağ: Bağlantı Durum Noktası & Batarya
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    BatteryIndicator()
                }
            }

            // ==================== 2: ÜST SATIR (CPU, GPU, RAM - 3 KART) ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.35f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CpuCard(cpu = stats.cpu, modifier = Modifier.weight(1f).fillMaxHeight())
                GpuCard(
                    gpu = activeGpu,
                    allGpus = allGpus,
                    selectedIndex = selectedGpuIndex,
                    onSelectGpu = { selectedGpuIndex = it },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                RamCard(ram = stats.ram, modifier = Modifier.weight(1f).fillMaxHeight())
            }

            // ==================== 2.5: ORTA KISIM (SİSTEM FANLARI - SYS BARI) ====================
            SysFansBar(
                fans = stats.fans,
                modifier = Modifier.fillMaxWidth()
            )

            // ==================== 3: ALT SATIR (DEPOLAMA KARTI) ====================
            StorageCard(
                disks = stats.disks,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}


// ==================== CPU KARTI ====================
@Composable
private fun CpuCard(cpu: CpuStats, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF12141C))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF7C2D12)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CPU",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cpu.name,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Gösterge + Sağ Bilgiler (GPU ve RAM gibi dairesel ark barı + sağ tarafta dikey metrikler)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularArcGauge(
                    percent = cpu.usage,
                    primaryColor = Color(0xFFF97316),
                    secondaryColor = Color(0xFFFB923C),
                    centerValue = "${cpu.usage.toInt()}%",
                    label = "Kullanım",
                    modifier = Modifier.size(76.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricRow(label = "Sıcaklık", value = if (cpu.temp > 0) "${cpu.temp.toInt()}°C" else "--°C")
                    MetricRow(label = "Frekans", value = if (cpu.clockMhz > 0) "${cpu.clockMhz.toInt()} MHz" else "--")
                    MetricRow(label = "Fan", value = if (cpu.fanRpm > 0) "${cpu.fanRpm.toInt()} RPM" else "--")
                    if (cpu.pumpRpm > 0) {
                        MetricRow(label = "Pompa", value = "${cpu.pumpRpm.toInt()} RPM")
                    }
                }
            }

            // Canlı Dalga Formu Grafiği
            WaveformChart(
                history = cpu.history,
                lineColor = Color(0xFFF97316),
                fillColor = Color(0xFFF97316),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )
        }
    }
}

// ==================== GPU KARTI ====================
@Composable
private fun GpuCard(
    gpu: GpuStats,
    allGpus: List<GpuStats>,
    selectedIndex: Int,
    onSelectGpu: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF12141C))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF4C1D95)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎮", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GPU",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (allGpus.size > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onSelectGpu((selectedIndex + 1) % allGpus.size) }
                                    .background(Color(0xFF2E1065))
                                    .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "GPU ${selectedIndex + 1}/${allGpus.size} ⟳",
                                    color = Color(0xFFD8B4FE),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = gpu.name,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Gösterge + Sağ Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularArcGauge(
                    percent = gpu.usage,
                    primaryColor = Color(0xFFA855F7),
                    secondaryColor = Color(0xFF818CF8),
                    centerValue = "${gpu.usage.toInt()}%",
                    label = "Kullanım",
                    modifier = Modifier.size(76.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricRow(label = "Sıcaklık", value = if (gpu.temp > 0) "${gpu.temp.toInt()}°C" else "--°C")
                    MetricRow(label = "VRAM", value = "${gpu.vramUsedGb} / ${gpu.vramTotalGb} GB")
                    MetricRow(label = "Fan", value = if (gpu.fanRpm > 0) "${gpu.fanRpm.toInt()} RPM" else "0 RPM")
                }
            }

            // Canlı Dalga Formu
            WaveformChart(
                history = gpu.history,
                lineColor = Color(0xFFA855F7),
                fillColor = Color(0xFFA855F7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )
        }
    }
}

// ==================== RAM KARTI ====================
@Composable
private fun RamCard(ram: RamStats, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF12141C))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0369A1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💾", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RAM",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ram.totalGb.toInt()} GB Bellek" + if (ram.sockets.isNotEmpty()) " (${ram.sockets.size} Slot)" else "",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                }
            }

            // Gösterge + Sağ Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularArcGauge(
                    percent = ram.usage,
                    primaryColor = Color(0xFF38BDF8),
                    secondaryColor = Color(0xFF0284C7),
                    centerValue = "${ram.usage.toInt()}%",
                    subValue = "${ram.usedGb} GB",
                    label = "Bellek",
                    modifier = Modifier.size(76.dp)
                )

                val avgTemp = if (ram.temp > 0) ram.temp
                              else if (ram.sockets.any { it.temp > 0 }) ram.sockets.filter { it.temp > 0 }.map { it.temp }.average().toFloat()
                              else 0f

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricRow(label = "Ort. Sıcaklık", value = if (avgTemp > 0) "${avgTemp.toInt()}°C" else "--°C")
                    MetricRow(label = "Frekans", value = if (ram.clockMhz > 0) "${ram.clockMhz.toInt()} MHz" else "--")
                    MetricRow(label = "Kullanılan", value = "${ram.usedGb} GB")
                    MetricRow(label = "Toplam", value = "${ram.totalGb} GB")
                }
            }

            // Canlı Dalga Formu
            WaveformChart(
                history = ram.history,
                lineColor = Color(0xFF38BDF8),
                fillColor = Color(0xFF38BDF8),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )
        }
    }
}

// ==================== SYS FANLARI BARI ====================
@Composable
private fun SysFansBar(fans: List<FanItemStats>, modifier: Modifier = Modifier) {
    val readableFans = remember(fans) { fans.filter { it.rpm > 0 } }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10141E))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(text = "🌪️", fontSize = 11.sp)
            Text(
                text = "SYS Fanları:",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (readableFans.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                items(readableFans) { fan ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF182030))
                            .border(1.dp, Color(0xFF26334D), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${fan.name}: ",
                            color = Color(0xFFCBD5E1),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${fan.rpm.toInt()} RPM",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Aktif devir okunan SYS fanı bulunamadı",
                color = Color(0xFF64748B),
                fontSize = 9.5.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ==================== DEPOLAMA KARTI ====================
@Composable
private fun StorageCard(disks: List<DiskItemStats>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF12141C))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💾 Depolama Aygıtları",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${disks.size} Sürücü",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            // Diskler Yatay Sıralama
            if (disks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Disk bilgisi alınıyor...", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            } else if (disks.size <= 4) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    disks.forEach { disk ->
                        DiskItemCard(
                            disk = disk,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(disks) { disk ->
                        DiskItemCard(
                            disk = disk,
                            modifier = Modifier.width(160.dp).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiskItemCard(disk: DiskItemStats, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181C26))
            .border(1.dp, Color(0xFF263045), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Sürücü İsmi & Sıcaklık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = disk.name.ifBlank { "Disk (${disk.driveLetter})" },
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (disk.temp > 0) "${disk.temp.toInt()}°C" else "--°C",
                    color = if (disk.temp > 0) Color(0xFFF59E0B) else Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Doluluk Barı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF242C3D))
            ) {
                val progressFraction = (disk.usagePercent / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                            )
                        )
                )
            }

            // Kapasite & Yüzde
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${disk.usedGb.toInt()}/${disk.totalGb.toInt()}GB",
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp
                )
                Text(
                    text = "%${disk.usagePercent.toInt()}",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Okuma / Yazma Hızları (Açıkça Okuma ve Yazma etiketleri)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Okuma: ",
                        color = Color(0xFF94A3B8),
                        fontSize = 7.5.sp
                    )
                    Text(
                        text = "${disk.readSpeedMb}M",
                        color = Color(0xFF4ADE80),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Yazma: ",
                        color = Color(0xFF94A3B8),
                        fontSize = 7.5.sp
                    )
                    Text(
                        text = "${disk.writeSpeedMb}M",
                        color = Color(0xFFF43F5E),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==================== YARDIMCI BİLEŞENLER ====================

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "$label:", color = Color(0xFF94A3B8), fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CircularArcGauge(
    percent: Float,
    primaryColor: Color,
    secondaryColor: Color,
    centerValue: String,
    subValue: String? = null,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Arka plan çizgisi: 240 derece
            drawArc(
                color = Color(0xFF1E2638),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // İlerleme çizgisi
            val sweep = (percent.coerceIn(0f, 100f) / 100f) * 240f
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(primaryColor, secondaryColor, primaryColor),
                        center = center
                    ),
                    startAngle = 150f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = centerValue,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (subValue != null) {
                Text(
                    text = subValue,
                    color = primaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WaveformChart(
    history: List<Float>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val maxVal = 100f
        val stepX = if (history.size > 1) w / (history.size - 1) else w

        val strokePath = Path()
        val fillPath = Path()

        fillPath.moveTo(0f, h)

        history.forEachIndexed { i, value ->
            val clamped = value.coerceIn(0f, maxVal)
            val x = i * stepX
            val y = h - (clamped / maxVal * h * 0.88f) - 2f

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevClamped = history[i - 1].coerceIn(0f, maxVal)
                val prevY = h - (prevClamped / maxVal * h * 0.88f) - 2f
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
                colors = listOf(fillColor.copy(alpha = 0.35f), fillColor.copy(alpha = 0.02f)),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

