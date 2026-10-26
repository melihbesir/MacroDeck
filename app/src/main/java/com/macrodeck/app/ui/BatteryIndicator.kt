package com.macrodeck.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun BatteryIndicator(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100f / scale).roundToInt().coerceIn(0, 100)
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        stickyIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100f / scale).roundToInt().coerceIn(0, 100)
            }
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val batteryColor = when {
        isCharging -> Color(0xFF38BDF8) // Şarj olurken Neon Cyan
        batteryLevel > 20 -> Color(0xFF22C55E) // %20 üzeri Yeşil
        batteryLevel > 10 -> Color(0xFFF59E0B) // %10-%20 arası Sarı/Amber
        else -> Color(0xFFEF4444) // %10 altı Kırmızı
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Yüzdelik Metin (Örn: %85)
        Text(
            text = "%$batteryLevel",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )

        // Pil İkonu (Doluluk oranına göre dinamik dolan kutu)
        Box(
            modifier = Modifier
                .width(23.dp)
                .height(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 1.2.dp.toPx()
                val tipWidth = 2.dp.toPx()
                val tipHeight = 5.dp.toPx()
                val bodyWidth = size.width - tipWidth - 1.dp.toPx()
                val bodyHeight = size.height

                // Pil gövdesi dış çerçevesi
                drawRoundRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(0f, 0f),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(2.5.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )

                // Pil artı kutup ucu (terminal)
                drawRoundRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(bodyWidth + 1.dp.toPx(), (bodyHeight - tipHeight) / 2f),
                    size = Size(tipWidth, tipHeight),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )

                // İç doluluk miktarı
                val padding = 1.8.dp.toPx()
                val innerMaxW = bodyWidth - (padding * 2)
                val innerH = bodyHeight - (padding * 2)
                val fillFraction = (batteryLevel / 100f).coerceIn(0f, 1f)
                val fillW = (innerMaxW * fillFraction).coerceAtLeast(0.5f)

                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(padding, padding),
                    size = Size(fillW, innerH),
                    cornerRadius = CornerRadius(1.2.dp.toPx())
                )
            }

            // Şarj oluyorsa ortasında küçük şimşek simgesi
            if (isCharging) {
                Text(
                    text = "⚡",
                    fontSize = 7.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
