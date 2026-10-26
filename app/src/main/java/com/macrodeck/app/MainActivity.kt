package com.macrodeck.app

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.google.gson.Gson
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.macrodeck.app.model.GridConfig
import com.macrodeck.app.model.MarketItemConfig
import com.macrodeck.app.model.SystemStats
import com.macrodeck.app.model.WeatherData
import com.macrodeck.app.network.DeckWebSocketClient
import com.macrodeck.app.network.WeatherRepository
import com.macrodeck.app.ui.DeckScreen
import com.macrodeck.app.ui.MarketScreen
import com.macrodeck.app.ui.ProcessMonitorScreen
import com.macrodeck.app.ui.QrScannerScreen
import com.macrodeck.app.ui.SystemMonitorScreen
import com.macrodeck.app.ui.WeatherScreen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var webSocketClient: DeckWebSocketClient
    private val prefs by lazy { getSharedPreferences("macro_deck_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Ekranın Kilitlenmesini Engelle (Sürekli Açık Kalma)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Oyun Modu Tam Ekran (Immersive Sticky - Durum ve Gezinme Çubuğu Gizli)
        enableImmersiveGameMode()

        setContent {
            var isConnected by remember { mutableStateOf(false) }
            var connectionStatusText by remember { mutableStateOf<String?>("Bağlantı kuruluyor...") }
            val gson = remember { Gson() }
            val cachedGridJson = remember { prefs.getString("cached_grid_config", null) }
            val initialGrid = remember(cachedGridJson) {
                if (!cachedGridJson.isNullOrBlank()) {
                    try {
                        gson.fromJson(cachedGridJson, GridConfig::class.java)
                    } catch (_: Exception) { null }
                } else null
            }

            var gridConfig by remember { mutableStateOf(initialGrid ?: GridConfig()) }
            var currentVolume by remember { mutableStateOf(initialGrid?.currentVolume ?: 50) }
            var showQrScanner by remember { mutableStateOf(false) }
            var showWeatherScreen by remember { mutableStateOf(false) }
            var showMarketScreen by remember { mutableStateOf(false) }
            var showSystemScreen by remember { mutableStateOf(false) }
            var showProcessScreen by remember { mutableStateOf(false) }
            var targetProcessName by remember { mutableStateOf("") }
            val cachedWeather = remember { WeatherRepository.loadCachedWeather(this@MainActivity) }

            var weatherData by remember { mutableStateOf(cachedWeather ?: WeatherData()) }
            var marketItems by remember {
                mutableStateOf(
                    if (!initialGrid?.marketItems.isNullOrEmpty()) initialGrid!!.marketItems else emptyList()
                )
            }
            var systemStats by remember { mutableStateOf(SystemStats()) }
            var processStats by remember { mutableStateOf(com.macrodeck.app.model.ProcessStats()) }
            var processList by remember { mutableStateOf<List<com.macrodeck.app.model.ProcessItemInfo>>(emptyList()) }
            var currentTargetIp by remember { mutableStateOf("") }
            var showExitDialog by remember { mutableStateOf(false) }

            // 1. Telefon Tarafında Bağımsız Hava Durumu (Arka planda 30 dakikada bir güncelleme)
            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) {
                    while (isActive) {
                        try {
                            val city = WeatherRepository.getSavedCity(this@MainActivity)
                            val freshWeather = WeatherRepository.fetchWeather(city)
                            if (freshWeather != null) {
                                withContext(Dispatchers.Main) {
                                    weatherData = freshWeather
                                }
                                WeatherRepository.saveCachedWeather(this@MainActivity, freshWeather)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Phone weather update error: ${e.message}")
                        }
                        // 30 dakikada bir otomatik güncelle
                        delay(30 * 60 * 1000L)
                    }
                }
            }

            // 2. WebSocket İstemcisini Başlat
            LaunchedEffect(Unit) {
                webSocketClient = DeckWebSocketClient(
                    onConnectionChanged = { connected, errorMsg ->
                        isConnected = connected
                        connectionStatusText = if (connected) null else (errorMsg ?: "Bağlantı yok")
                    },
                    onConfigReceived = { newConfig ->
                        gridConfig = newConfig
                        currentVolume = newConfig.currentVolume
                        if (newConfig.marketItems.isNotEmpty()) {
                            marketItems = newConfig.marketItems
                        }
                        // Ayarları telefona kaydet: PC kapalıyken bile ekranda gözüksün
                        try {
                            val json = gson.toJson(newConfig)
                            prefs.edit().putString("cached_grid_config", json).apply()
                        } catch (_: Exception) {}
                    },
                    onVolumeSync = { newVolume ->
                        currentVolume = newVolume
                    },
                    onWeatherReceived = { newWeather ->
                        weatherData = newWeather
                        WeatherRepository.saveCachedWeather(this@MainActivity, newWeather)
                    },
                    onMarketConfigReceived = { newItems ->
                        marketItems = newItems
                        try {
                            val updated = gridConfig.copy(marketItems = newItems)
                            gridConfig = updated
                            val json = gson.toJson(updated)
                            prefs.edit().putString("cached_grid_config", json).apply()
                        } catch (_: Exception) {}
                    },
                    onSystemStatsReceived = { newStats ->
                        systemStats = newStats
                    },
                    onProcessStatsReceived = { newProcStats ->
                        processStats = newProcStats
                    },
                    onProcessListReceived = { newProcList ->
                        processList = newProcList
                    }
                )

                // Kayıtlı IP var mı kontrol et, varsa doğrudan bağlan
                val savedIp = prefs.getString("pc_ip", null)
                val savedPort = prefs.getInt("pc_port", 8765)

                if (!savedIp.isNullOrBlank()) {
                    currentTargetIp = savedIp
                    webSocketClient.connect(savedIp, savedPort)
                } else {
                    // İlk açılışta QR tarayıcıyı aç
                    showQrScanner = true
                }
            }

            if (showQrScanner) {
                QrScannerScreen(
                    onQrScanned = { ip, port ->
                        currentTargetIp = ip
                        prefs.edit()
                            .putString("pc_ip", ip)
                            .putInt("pc_port", port)
                            .apply()

                        showQrScanner = false
                        webSocketClient.connect(ip, port)
                    },
                    onCancel = {
                        showQrScanner = false
                    }
                )
            } else if (showWeatherScreen) {
                WeatherScreen(
                    weather = weatherData,
                    onBack = {
                        showWeatherScreen = false
                    }
                )
            } else if (showMarketScreen) {
                MarketScreen(
                    items = marketItems,
                    onBack = {
                        showMarketScreen = false
                    }
                )
            } else if (showSystemScreen) {
                SystemMonitorScreen(
                    stats = systemStats,
                    isConnected = isConnected,
                    onBack = {
                        showSystemScreen = false
                        webSocketClient.sendStopSystemMonitor()
                    }
                )
            } else if (showProcessScreen) {
                ProcessMonitorScreen(
                    processStats = processStats,
                    processList = processList,
                    targetProcessName = targetProcessName,
                    isConnected = isConnected,
                    onSelectProcess = { pid -> webSocketClient.sendSelectProcess(pid) },
                    onRefreshProcessList = { webSocketClient.sendGetProcessList() },
                    onBack = {
                        showProcessScreen = false
                        webSocketClient.sendStopProcessMonitor()
                    }
                )
            } else {
                DeckScreen(
                    config = gridConfig,
                    isConnected = isConnected,
                    currentVolume = currentVolume,
                    currentIp = currentTargetIp,
                    statusError = connectionStatusText,
                    onButtonPressed = { buttonId ->
                        val btn = gridConfig.buttons.find { it.id == buttonId }
                        if (btn?.actionType == "weather") {
                            showWeatherScreen = true
                            if (isConnected) {
                                webSocketClient.sendButtonPress(buttonId)
                            }
                        } else if (btn?.actionType == "market") {
                            val targetItems = if (!btn.marketItems.isNullOrEmpty()) btn.marketItems else gridConfig.marketItems
                            marketItems = targetItems
                            showMarketScreen = true
                            if (isConnected) {
                                webSocketClient.sendButtonPress(buttonId)
                            }
                        } else if (btn?.actionType == "system") {
                            if (isConnected) {
                                showSystemScreen = true
                                webSocketClient.sendButtonPress(buttonId)
                                val interval = if (btn.systemInterval > 0) btn.systemInterval else 1.0
                                webSocketClient.sendStartSystemMonitor(interval)
                            } else {
                                Toast.makeText(this@MainActivity, "Sistem monitörü için bilgisayar bağlantısı gerekiyor", Toast.LENGTH_SHORT).show()
                            }
                        } else if (btn?.actionType == "process") {
                            if (isConnected) {
                                targetProcessName = btn.targetProcess
                                showProcessScreen = true
                                webSocketClient.sendButtonPress(buttonId)
                                webSocketClient.sendStartProcessMonitor(btn.targetProcess)
                                webSocketClient.sendGetProcessList()
                            } else {
                                Toast.makeText(this@MainActivity, "Uygulama takibi için bilgisayar bağlantısı gerekiyor", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (isConnected) {
                                webSocketClient.sendButtonPress(buttonId)
                            } else {
                                Toast.makeText(this@MainActivity, "Bilgisayar bağlantısı yok", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onVolumeChanged = { newVol ->
                        currentVolume = newVol
                        webSocketClient.sendVolumeChange(newVol)
                    },
                    onOpenQrScanner = {
                        showQrScanner = true
                    }
                )
            }

            // ==================== SİSTEM GERİ TUŞU: ANA MENÜDE ÇIKIŞ ONAYI ====================
            val isAtMainMenu = !showQrScanner && !showWeatherScreen && !showMarketScreen && !showSystemScreen && !showProcessScreen


            BackHandler(enabled = isAtMainMenu) {
                showExitDialog = true
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    containerColor = Color(0xFF141824),
                    titleContentColor = Color.White,
                    textContentColor = Color(0xFF94A3B8),
                    shape = RoundedCornerShape(14.dp),
                    title = {
                        Text(
                            text = "Mobile Deck and Monitör'den Çıkılsın mı?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Uygulamayı kapatmak istediğinizden emin misiniz?",
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Çıkış Yap", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showExitDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İptal", fontSize = 12.sp)
                        }
                    }
                )
            }
        }
    }

    private fun enableImmersiveGameMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveGameMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::webSocketClient.isInitialized) {
            webSocketClient.disconnect()
        }
    }
}
