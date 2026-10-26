package com.macrodeck.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrodeck.app.model.DeckButton
import com.macrodeck.app.model.GridConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DeckScreen(
    config: GridConfig,
    isConnected: Boolean,
    currentVolume: Int,
    currentIp: String = "",
    statusError: String? = null,
    onButtonPressed: (String) -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onOpenQrScanner: () -> Unit
) {
    // Canlı Tarih ve Saat
    var currentDate by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("dd MMMM", Locale("tr"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            val now = Date()
            currentDate = dateFormat.format(now)
            currentTime = timeFormat.format(now)
            delay(1000)
        }
    }

    // Saf Siyah Arka Plan (#000000 - AMOLED dostu)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==================== ÜST BİLGİ ALANI ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Bağlantı Durumu Noktası & Durum Metni & QR Butonu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "PC Bağlı" else "PC Çevrimdışı",
                        color = if (isConnected) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onOpenQrScanner,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Tara",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Sağ: Gün-Ay Tarih, Canlı Saat ve Telefon Batarya Göstergesi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentDate,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = currentTime,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    BatteryIndicator()
                }
            }

            // ==================== ANA ALAN (5x4 GRID + SAĞ SES BARI) ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Sol & Orta: 5x4 Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    ButtonsGrid(
                        config = config,
                        isConnected = isConnected,
                        onButtonPressed = onButtonPressed
                    )
                }

                // En Sağ: Dikey Ses Barı + Altta % Göstergesi
                VolumeVerticalBar(
                    volume = currentVolume,
                    isEnabled = isConnected,
                    onVolumeChanged = onVolumeChanged,
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun ButtonsGrid(
    config: GridConfig,
    isConnected: Boolean,
    onButtonPressed: (String) -> Unit
) {
    val cols = config.columns.coerceIn(3, 5)
    val rows = config.rows.coerceIn(3, 5)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until cols) {
                    val btn = config.buttons.find { it.row == r && it.col == c }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (btn != null) {
                            DeckButtonCell(
                                button = btn,
                                isConnected = isConnected,
                                isCompact = (rows >= 5 || cols >= 5),
                                onClick = { onButtonPressed(btn.id) }
                            )
                        } else {
                            // Boş Grid Hücresi
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F1117))
                                    .border(1.dp, Color(0xFF1E222D), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeckButtonCell(
    button: DeckButton,
    isConnected: Boolean,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    // PC olmadan çalışabilen bağımsız eylemler: Hava Durumu ve Piyasa Takibi
    val isStandalone = (button.actionType == "weather" || button.actionType == "market")
    val isEnabled = isConnected || isStandalone

    val bgColor = remember(button.backgroundColor) {
        try {
            Color(android.graphics.Color.parseColor(button.backgroundColor))
        } catch (_: Exception) {
            Color(0xFF1E222D)
        }
    }

    val iconBitmap = remember(button.iconBase64) {
        button.iconBase64?.let { base64 ->
            try {
                val decoded = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val iconSize = if (isCompact) 32.dp else 44.dp
    val fontSize = if (isCompact) 11.sp else 13.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (isEnabled) 1f else 0.38f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isEnabled) Color(0xFF334155) else Color(0xFF1E2430),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = button.title,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(bottom = 2.dp)
                )
            } else if (button.actionType == "weather") {
                Text(
                    text = "⛅",
                    fontSize = if (isCompact) 22.sp else 28.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            } else if (button.actionType == "market") {
                Text(
                    text = "📈",
                    fontSize = if (isCompact) 22.sp else 28.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (button.title.isNotBlank()) {
                Text(
                    text = button.title,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VolumeVerticalBar(
    volume: Int,
    isEnabled: Boolean = true,
    onVolumeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun updateVolumeFromTouch(y: Float) {
        if (isEnabled && size.height > 0) {
            val fraction = 1f - (y / size.height.toFloat()).coerceIn(0f, 1f)
            val newVol = (fraction * 100).roundToInt()
            onVolumeChanged(newVol)
        }
    }

    Column(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.38f)
            .background(Color.Transparent)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst Ses Simgesi
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Ses",
            tint = Color(0xFF64748B),
            modifier = Modifier
                .size(16.dp)
                .padding(bottom = 4.dp)
        )

        // Dikey Kaydırılabilir Bar (Slider Alanı)
        Box(
            modifier = Modifier
                .weight(1f)
                .width(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1E222D))
                .onSizeChanged { size = it }
                .then(
                    if (isEnabled) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    updateVolumeFromTouch(offset.y)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    updateVolumeFromTouch(change.position.y)
                                }
                            }
                    } else Modifier
                )
        ) {
            val fillFraction = (volume.coerceIn(0, 100) / 100f)

            // Doldurulan Alan (Aşağıdan Yukarıya)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (isEnabled) listOf(Color(0xFF38BDF8), Color(0xFF0284C7)) else listOf(Color(0xFF475569), Color(0xFF334155))
                        )
                    )
            )
        }

        // Alt Kısım: % Ses Seviyesi Göstergesi (Örn: %30, %40)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E293B))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEnabled) "%$volume" else "%--",
                color = if (isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
