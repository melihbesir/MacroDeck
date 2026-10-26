package com.macrodeck.app.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.macrodeck.app.model.DayForecast
import com.macrodeck.app.model.HourlyForecast
import com.macrodeck.app.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object WeatherRepository {
    private const val TAG = "WeatherRepository"
    private const val PREFS_NAME = "macro_deck_weather_prefs"
    private const val KEY_CACHED_WEATHER = "cached_weather_json"
    private const val KEY_CITY = "weather_city"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * SharedPreferences'tan önbelleğe alınmış hava durumunu hızlıca döner
     */
    fun loadCachedWeather(context: Context): WeatherData? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_CACHED_WEATHER, null)
            if (!json.isNullOrBlank()) {
                gson.fromJson(json, WeatherData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached weather: ${e.message}")
            null
        }
    }

    /**
     * Alınan hava durumu bilgisini SharedPreferences'a önbelleğe yazar
     */
    fun saveCachedWeather(context: Context, data: WeatherData) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(data)
            prefs.edit().putString(KEY_CACHED_WEATHER, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cached weather: ${e.message}")
        }
    }

    /**
     * Kayıtlı şehri döner (varsayılan İstanbul)
     */
    fun getSavedCity(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CITY, "İstanbul") ?: "İstanbul"
    }

    /**
     * Şehir tercihini günceller
     */
    fun saveCity(context: Context, city: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CITY, city.trim()).apply()
    }

    /**
     * PC'ye ihtiyaç duymadan doğrudan Open-Meteo servislerinden hava durumu verisini çeker
     */
    suspend fun fetchWeather(cityName: String): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val targetCity = cityName.ifBlank { "İstanbul" }

            // 1. Geocoding: Şehir koordinatlarını bul
            var lat = 41.01
            var lon = 28.97
            var resolvedName = targetCity

            try {
                val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${Uri.encode(targetCity)}&count=1&language=tr&format=json"
                val geoReq = Request.Builder().url(geoUrl).build()
                client.newCall(geoReq).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    val geoDoc = gson.fromJson(body, JsonObject::class.java)
                    val results = geoDoc.getAsJsonArray("results")
                    if (results != null && results.size() > 0) {
                        val first = results.get(0).asJsonObject
                        lat = first.get("latitude")?.asDouble ?: lat
                        lon = first.get("longitude")?.asDouble ?: lon
                        resolvedName = first.get("name")?.asString ?: targetCity
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoding fallback for $targetCity: ${e.message}")
            }

            // 2. Open-Meteo Forecast: 7 Günlük ve Saatlik Tahminleri Çek
            val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&hourly=temperature_2m,precipitation_probability,wind_speed_10m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto"
            val forecastReq = Request.Builder().url(forecastUrl).build()

            client.newCall(forecastReq).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                val root = gson.fromJson(body, JsonObject::class.java)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val lastUpdated = timeFormat.format(Date())

                // Günlük tahminler listesi
                val daysList = mutableListOf<DayForecast>()
                var currentPrecipitation = 0

                val daily = root.getAsJsonObject("daily")
                if (daily != null) {
                    val dates = daily.getAsJsonArray("time") ?: JsonArray()
                    val maxTemps = daily.getAsJsonArray("temperature_2m_max") ?: JsonArray()
                    val minTemps = daily.getAsJsonArray("temperature_2m_min") ?: JsonArray()
                    val weatherCodes = daily.getAsJsonArray("weather_code") ?: JsonArray()
                    val precs = daily.getAsJsonArray("precipitation_probability_max") ?: JsonArray()

                    val trLocale = Locale("tr", "TR")
                    val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("EEE", trLocale)

                    val count = minOf(dates.size(), 7)
                    for (i in 0 until count) {
                        val dateStr = dates.get(i)?.asString ?: ""
                        var dayName = "Gün"
                        try {
                            val parsedDate = parseFormat.parse(dateStr)
                            if (parsedDate != null) {
                                val abbr = dayFormat.format(parsedDate)
                                dayName = if (abbr.length > 3) abbr.substring(0, 3) else abbr
                                dayName = dayName.replaceFirstChar { it.uppercase() }
                            }
                        } catch (_: Exception) {}

                        val wCode = weatherCodes.get(i)?.asInt ?: 0
                        val maxT = maxTemps.get(i)?.asDouble?.roundToInt() ?: 20
                        val minT = minTemps.get(i)?.asDouble?.roundToInt() ?: 12

                        daysList.add(
                            DayForecast(
                                date = dateStr,
                                dayName = dayName,
                                maxTemp = maxT,
                                minTemp = minT,
                                condition = getWeatherDescription(wCode),
                                weatherCode = wCode,
                                hourly = mutableListOf()
                            )
                        )
                    }

                    if (precs.size() > 0) {
                        currentPrecipitation = precs.get(0)?.asInt ?: 0
                    }
                }

                // Saatlik tahminler listesi
                val hourly = root.getAsJsonObject("hourly")
                if (hourly != null && daysList.isNotEmpty()) {
                    val times = hourly.getAsJsonArray("time") ?: JsonArray()
                    val temps = hourly.getAsJsonArray("temperature_2m") ?: JsonArray()
                    val precs = hourly.getAsJsonArray("precipitation_probability") ?: JsonArray()
                    val winds = hourly.getAsJsonArray("wind_speed_10m") ?: JsonArray()
                    val codes = hourly.getAsJsonArray("weather_code") ?: JsonArray()

                    val targetHours = intArrayOf(2, 5, 8, 11, 14, 17, 20, 23)

                    for (d in daysList.indices) {
                        val startIndex = d * 24
                        val dayHourlyList = mutableListOf<HourlyForecast>()

                        for (h in targetHours) {
                            val idx = startIndex + h
                            if (idx < times.size()) {
                                val t = temps.get(idx)?.asDouble?.roundToInt() ?: 0
                                val p = precs.get(idx)?.asInt ?: 0
                                val w = winds.get(idx)?.asDouble?.roundToInt() ?: 0
                                val c = codes.get(idx)?.asInt ?: 0

                                dayHourlyList.add(
                                    HourlyForecast(
                                        time = String.format(Locale.ROOT, "%02d:00", h),
                                        temp = t,
                                        precipitation = p,
                                        windSpeed = w,
                                        weatherCode = c
                                    )
                                )
                            }
                        }

                        // Günün saatlik tahminlerini güncelle
                        daysList[d] = daysList[d].copy(hourly = dayHourlyList)
                    }
                }

                // Mevcut Anlık Hava Durumu
                var currentTemp = 18
                var currentHumidity = 65
                var currentWindSpeed = 10
                var currentWeatherCode = 3

                val current = root.getAsJsonObject("current")
                if (current != null) {
                    currentTemp = current.get("temperature_2m")?.asDouble?.roundToInt() ?: 18
                    currentHumidity = current.get("relative_humidity_2m")?.asInt ?: 65
                    currentWindSpeed = current.get("wind_speed_10m")?.asDouble?.roundToInt() ?: 10
                    currentWeatherCode = current.get("weather_code")?.asInt ?: 3
                }

                WeatherData(
                    cityName = resolvedName,
                    currentTemp = currentTemp,
                    currentCondition = getWeatherDescription(currentWeatherCode),
                    currentWeatherCode = currentWeatherCode,
                    humidity = currentHumidity,
                    windSpeed = currentWindSpeed,
                    precipitation = currentPrecipitation,
                    lastUpdated = lastUpdated,
                    days = daysList
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather directly on phone: ${e.message}")
            null
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Açık"
            1 -> "Çoğunlukla açık"
            2 -> "Parçalı bulutlu"
            3 -> "Çok bulutlu"
            45, 48 -> "Sisli"
            51, 53, 55 -> "Çiseleyen yağmur"
            61, 63, 65 -> "Yağmurlu"
            66, 67 -> "Dondurucu yağmur"
            71, 73, 75 -> "Karlı"
            77 -> "Kar taneleri"
            80, 81, 82 -> "Sağanak yağış"
            85, 86 -> "Kar sağanağı"
            95 -> "Gök gürültülü fırtına"
            96, 99 -> "Dolu ve fırtına"
            else -> "Bulutlu"
        }
    }
}
