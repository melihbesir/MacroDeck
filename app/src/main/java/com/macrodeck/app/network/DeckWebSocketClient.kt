package com.macrodeck.app.network

import android.util.Log
import com.google.gson.Gson
import com.macrodeck.app.model.GridConfig
import com.macrodeck.app.model.MarketItemConfig
import com.macrodeck.app.model.SocketMessage
import com.macrodeck.app.model.SystemStats
import com.macrodeck.app.model.WeatherData
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class DeckWebSocketClient(
    private val onConnectionChanged: (Boolean, String?) -> Unit,
    private val onConfigReceived: (GridConfig) -> Unit,
    private val onVolumeSync: (Int) -> Unit,
    private val onWeatherReceived: (WeatherData) -> Unit = {},
    private val onMarketConfigReceived: (List<MarketItemConfig>) -> Unit = {},
    private val onSystemStatsReceived: (SystemStats) -> Unit = {},
    private val onProcessStatsReceived: (com.macrodeck.app.model.ProcessStats) -> Unit = {},
    private val onProcessListReceived: (List<com.macrodeck.app.model.ProcessItemInfo>) -> Unit = {}
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var currentIp: String? = null
    private var currentPort: Int = 8765

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var shouldReconnect = true

    fun connect(ip: String, port: Int = 8765) {
        val cleanIp = ip.trim()
            .removePrefix("ws://")
            .removePrefix("http://")
            .trimEnd('/')

        currentIp = cleanIp
        currentPort = port
        shouldReconnect = true

        val url = "ws://$cleanIp:$port"
        Log.d("MacroDeck", "Connecting to WebSocket: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket?.close(1000, "Reconnecting")
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("MacroDeck", "WebSocket Connected Successfully to $url")
                isConnected = true
                scope.launch(Dispatchers.Main) {
                    onConnectionChanged(true, null)
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("MacroDeck", "WebSocket Received: $text")
                handleMessage(text)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.w("MacroDeck", "WebSocket Closed: $code - $reason")
                isConnected = false
                scope.launch(Dispatchers.Main) {
                    onConnectionChanged(false, "Bağlantı kapandı ($code): $reason")
                }
                triggerReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("MacroDeck", "WebSocket Failure: ${t.message}", t)
                isConnected = false
                val errorMsg = t.localizedMessage ?: t.message ?: "Bilinmeyen ağ hatası"
                scope.launch(Dispatchers.Main) {
                    onConnectionChanged(false, errorMsg)
                }
                triggerReconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val jsonObject = gson.fromJson(text, Map::class.java)
            val event = jsonObject["event"] as? String ?: return

            when (event) {
                "GRID_CONFIG" -> {
                    val config = gson.fromJson(text, GridConfig::class.java)
                    scope.launch(Dispatchers.Main) {
                        onConfigReceived(config)
                        if (config.marketItems.isNotEmpty()) {
                            onMarketConfigReceived(config.marketItems)
                        }
                    }
                }
                "VOLUME_SYNC" -> {
                    val value = (jsonObject["value"] as? Number)?.toInt() ?: return
                    scope.launch(Dispatchers.Main) {
                        onVolumeSync(value)
                    }
                }
                "WEATHER_DATA" -> {
                    val dataJson = gson.toJson(jsonObject["data"])
                    val weather = gson.fromJson(dataJson, WeatherData::class.java)
                    if (weather != null) {
                        scope.launch(Dispatchers.Main) {
                            onWeatherReceived(weather)
                        }
                    }
                }
                "MARKET_CONFIG" -> {
                    val itemsJson = gson.toJson(jsonObject["items"])
                    val itemsType = object : com.google.gson.reflect.TypeToken<List<MarketItemConfig>>() {}.type
                    val items: List<MarketItemConfig>? = gson.fromJson(itemsJson, itemsType)
                    if (items != null) {
                        scope.launch(Dispatchers.Main) {
                            onMarketConfigReceived(items)
                        }
                    }
                }
                "SYSTEM_STATS" -> {
                    val dataJson = gson.toJson(jsonObject["data"])
                    val stats = gson.fromJson(dataJson, SystemStats::class.java)
                    if (stats != null) {
                        scope.launch(Dispatchers.Main) {
                            onSystemStatsReceived(stats)
                        }
                    }
                }
                "PROCESS_STATS" -> {
                    val dataJson = gson.toJson(jsonObject["data"])
                    val stats = gson.fromJson(dataJson, com.macrodeck.app.model.ProcessStats::class.java)
                    if (stats != null) {
                        scope.launch(Dispatchers.Main) {
                            onProcessStatsReceived(stats)
                        }
                    }
                }
                "PROCESS_LIST" -> {
                    val procJson = gson.toJson(jsonObject["processes"])
                    val listType = object : com.google.gson.reflect.TypeToken<List<com.macrodeck.app.model.ProcessItemInfo>>() {}.type
                    val list: List<com.macrodeck.app.model.ProcessItemInfo>? = gson.fromJson(procJson, listType)
                    if (list != null) {
                        scope.launch(Dispatchers.Main) {
                            onProcessListReceived(list)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MacroDeck", "Error parsing message: ${e.message}", e)
        }
    }

    private fun triggerReconnect() {
        if (!shouldReconnect || currentIp == null) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (!isConnected && shouldReconnect && currentIp != null) {
                Log.d("MacroDeck", "Attempting automatic reconnect to $currentIp:$currentPort...")
                connect(currentIp!!, currentPort)
            }
        }
    }

    fun sendButtonPress(buttonId: String) {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "BUTTON_PRESS", id = buttonId)
        webSocket?.send(gson.toJson(msg))
    }

    fun sendVolumeChange(volumePercent: Int) {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "VOLUME_CHANGE", value = volumePercent)
        webSocket?.send(gson.toJson(msg))
    }

    fun sendStartSystemMonitor(interval: Double = 1.0) {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "START_SYSTEM_MONITOR", interval = interval)
        webSocket?.send(gson.toJson(msg))
    }

    fun sendStopSystemMonitor() {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "STOP_SYSTEM_MONITOR")
        webSocket?.send(gson.toJson(msg))
    }

    fun sendStartProcessMonitor(targetProcess: String? = null) {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "START_PROCESS_MONITOR", targetProcess = targetProcess)
        webSocket?.send(gson.toJson(msg))
    }


    fun sendStopProcessMonitor() {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "STOP_PROCESS_MONITOR")
        webSocket?.send(gson.toJson(msg))
    }

    fun sendSelectProcess(pid: Int) {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "SELECT_PROCESS", pid = pid)
        webSocket?.send(gson.toJson(msg))
    }

    fun sendGetProcessList() {
        if (!isConnected || webSocket == null) return
        val msg = SocketMessage(event = "GET_PROCESS_LIST")
        webSocket?.send(gson.toJson(msg))
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "App closed")
        isConnected = false
        onConnectionChanged(false, "Uygulama kapatıldı")
    }
}
