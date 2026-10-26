package com.macrodeck.app.model

import com.google.gson.annotations.SerializedName

data class DeckButton(
    @SerializedName("id") val id: String = "",
    @SerializedName("row") val row: Int = 0,
    @SerializedName("col") val col: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("actionType") val actionType: String = "app", // "app", "weather", "market", "system" or "process"
    @SerializedName("targetProcess") val targetProcess: String = "",
    @SerializedName("backgroundColor") val backgroundColor: String = "#1e222d",
    @SerializedName("textColor") val textColor: String = "#ffffff",
    @SerializedName("iconBase64") val iconBase64: String? = null,
    @SerializedName("marketItems") val marketItems: List<MarketItemConfig>? = null,
    @SerializedName("systemInterval") val systemInterval: Double = 1.0
)

data class MarketItemConfig(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("category") val category: String = "crypto", // "crypto", "bist", "gold"
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("color") val color: String = "#38BDF8"
)

data class MarketQuote(
    val id: String,
    val name: String,
    val symbol: String,
    val category: String,
    val color: String,
    val price: Double = 0.0,
    val changePercent: Double = 0.0,
    val history: List<Double> = emptyList(),
    val tickDirection: Int = 0 // 1 = up (green), -1 = down (red), 0 = stable
)

data class GridConfig(
    @SerializedName("columns") val columns: Int = 5,
    @SerializedName("rows") val rows: Int = 4,
    @SerializedName("cityName") val cityName: String = "İstanbul",
    @SerializedName("currentVolume") val currentVolume: Int = 50,
    @SerializedName("buttons") val buttons: List<DeckButton> = emptyList(),
    @SerializedName("marketItems") val marketItems: List<MarketItemConfig> = emptyList()
)

data class SocketMessage(
    @SerializedName("event") val event: String,
    @SerializedName("id") val id: String? = null,
    @SerializedName("value") val value: Int? = null,
    @SerializedName("interval") val interval: Double? = null,
    @SerializedName("pid") val pid: Int? = null,
    @SerializedName("targetProcess") val targetProcess: String? = null
)


data class ProcessStats(
    @SerializedName("pid") val pid: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("isAutoActiveWindow") val isAutoActiveWindow: Boolean = true,
    @SerializedName("cpuPercent") val cpuPercent: Float = 0f,
    @SerializedName("ramMb") val ramMb: Float = 0f,
    @SerializedName("ramPercent") val ramPercent: Float = 0f,
    @SerializedName("gpuPercent") val gpuPercent: Float = 0f,
    @SerializedName("diskReadMb") val diskReadMb: Float = 0f,
    @SerializedName("diskWriteMb") val diskWriteMb: Float = 0f,
    @SerializedName("diskPercent") val diskPercent: Float = 0f,
    @SerializedName("netRecvMb") val netRecvMb: Float = 0f,
    @SerializedName("netSendMb") val netSendMb: Float = 0f,
    @SerializedName("netPercent") val netPercent: Float = 0f,
    @SerializedName("cpuHistory") val cpuHistory: List<Float> = emptyList(),
    @SerializedName("ramHistory") val ramHistory: List<Float> = emptyList(),
    @SerializedName("gpuHistory") val gpuHistory: List<Float> = emptyList(),
    @SerializedName("diskHistory") val diskHistory: List<Float> = emptyList(),
    @SerializedName("netHistory") val netHistory: List<Float> = emptyList()
)

data class ProcessItemInfo(
    @SerializedName("pid") val pid: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("cpuPercent") val cpuPercent: Float = 0f,
    @SerializedName("ramMb") val ramMb: Float = 0f
)

data class CpuStats(
    @SerializedName("name") val name: String = "CPU",
    @SerializedName("usage") val usage: Float = 0f,
    @SerializedName("temp") val temp: Float = 0f,
    @SerializedName("clockMhz") val clockMhz: Float = 0f,
    @SerializedName("fanRpm") val fanRpm: Float = 0f,
    @SerializedName("pumpRpm") val pumpRpm: Float = 0f,
    @SerializedName("cores") val cores: List<Float> = emptyList(),
    @SerializedName("history") val history: List<Float> = emptyList()
)

data class GpuStats(
    @SerializedName("name") val name: String = "GPU",
    @SerializedName("usage") val usage: Float = 0f,
    @SerializedName("temp") val temp: Float = 0f,
    @SerializedName("vramUsedGb") val vramUsedGb: Float = 0f,
    @SerializedName("vramTotalGb") val vramTotalGb: Float = 0f,
    @SerializedName("fanRpm") val fanRpm: Float = 0f,
    @SerializedName("history") val history: List<Float> = emptyList()
)

data class RamSocketInfo(
    @SerializedName("locator") val locator: String = "",
    @SerializedName("capacityGb") val capacityGb: Float = 0f,
    @SerializedName("speedMhz") val speedMhz: Float = 0f,
    @SerializedName("temp") val temp: Float = 0f
)

data class RamStats(
    @SerializedName("name") val name: String = "RAM",
    @SerializedName("usage") val usage: Float = 0f,
    @SerializedName("usedGb") val usedGb: Float = 0f,
    @SerializedName("totalGb") val totalGb: Float = 0f,
    @SerializedName("clockMhz") val clockMhz: Float = 0f,
    @SerializedName("temp") val temp: Float = 0f,
    @SerializedName("sockets") val sockets: List<RamSocketInfo> = emptyList(),
    @SerializedName("history") val history: List<Float> = emptyList()
)

data class DiskItemStats(
    @SerializedName("name") val name: String = "",
    @SerializedName("driveLetter") val driveLetter: String = "",
    @SerializedName("usedGb") val usedGb: Float = 0f,
    @SerializedName("totalGb") val totalGb: Float = 0f,
    @SerializedName("usagePercent") val usagePercent: Float = 0f,
    @SerializedName("readSpeedMb") val readSpeedMb: Float = 0f,
    @SerializedName("writeSpeedMb") val writeSpeedMb: Float = 0f,
    @SerializedName("temp") val temp: Float = 0f
)

data class FanItemStats(
    @SerializedName("name") val name: String = "",
    @SerializedName("rpm") val rpm: Float = 0f
)

data class SystemStats(
    @SerializedName("cpu") val cpu: CpuStats = CpuStats(),
    @SerializedName("gpu") val gpu: GpuStats = GpuStats(),
    @SerializedName("gpus") val gpus: List<GpuStats> = emptyList(),
    @SerializedName("ram") val ram: RamStats = RamStats(),
    @SerializedName("disks") val disks: List<DiskItemStats> = emptyList(),
    @SerializedName("fans") val fans: List<FanItemStats> = emptyList()
)

data class HourlyForecast(
    @SerializedName("time") val time: String = "",
    @SerializedName("temp") val temp: Int = 0,
    @SerializedName("precipitation") val precipitation: Int = 0,
    @SerializedName("windSpeed") val windSpeed: Int = 0,
    @SerializedName("weatherCode") val weatherCode: Int = 0
)

data class DayForecast(
    @SerializedName("date") val date: String = "",
    @SerializedName("dayName") val dayName: String = "",
    @SerializedName("maxTemp") val maxTemp: Int = 0,
    @SerializedName("minTemp") val minTemp: Int = 0,
    @SerializedName("condition") val condition: String = "",
    @SerializedName("weatherCode") val weatherCode: Int = 0,
    @SerializedName("hourly") val hourly: List<HourlyForecast> = emptyList()
)

data class WeatherData(
    @SerializedName("cityName") val cityName: String = "İstanbul",
    @SerializedName("currentTemp") val currentTemp: Int = 19,
    @SerializedName("currentCondition") val currentCondition: String = "Çok bulutlu",
    @SerializedName("currentWeatherCode") val currentWeatherCode: Int = 3,
    @SerializedName("humidity") val humidity: Int = 77,
    @SerializedName("windSpeed") val windSpeed: Int = 8,
    @SerializedName("precipitation") val precipitation: Int = 10,
    @SerializedName("lastUpdated") val lastUpdated: String = "",
    @SerializedName("days") val days: List<DayForecast> = emptyList()
)
