package com.example.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WeatherSnapshot(
    val city: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val windKph: Double,
    val rainChancePercent: Int,
    val minC: Double,
    val maxC: Double,
    val condition: String,
    val isDay: Boolean,
    val sunrise: String,
    val sunset: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Bengali one-liner used for voice answers. */
    fun spokenSummary(): String {
        val rainText = if (rainChancePercent >= 40) " আজ বৃষ্টির সম্ভাবনা $rainChancePercent%।" else ""
        return "$city-তে এখন $condition, তাপমাত্রা ${temperatureC.toInt()} ডিগ্রি সেলসিয়াস " +
            "(অনুভূত হচ্ছে ${feelsLikeC.toInt()} ডিগ্রি), আজকের সর্বোচ্চ ${maxC.toInt()} ও সর্বনিম্ন ${minC.toInt()} ডিগ্রি।$rainText"
    }
}

/**
 * Real weather from the free Open-Meteo REST API (no API key required).
 */
class WeatherClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun geocode(city: String): Triple<Double, Double, String>? = withContext(Dispatchers.IO) {
        val query = city.trim().ifBlank { return@withContext null }
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
            java.net.URLEncoder.encode(query, "UTF-8") + "&count=1&format=json"
        val body = httpGet(url) ?: return@withContext null
        try {
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            val first = results.getJSONObject(0)
            Triple(
                first.optDouble("latitude", 0.0),
                first.optDouble("longitude", 0.0),
                first.optString("name", city)
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchByCoordinates(lat: Double, lon: Double, city: String): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&timezone=auto&forecast_days=1"
            val body = httpGet(url) ?: return@withContext null
            try {
                val root = JSONObject(body)
                val current = root.optJSONObject("current") ?: return@withContext null
                val daily = root.optJSONObject("daily")
                val code = current.optInt("weather_code", 0)
                WeatherSnapshot(
                    city = city,
                    temperatureC = current.optDouble("temperature_2m", 0.0),
                    feelsLikeC = current.optDouble("apparent_temperature", current.optDouble("temperature_2m", 0.0)),
                    humidityPercent = current.optInt("relative_humidity_2m", 0),
                    windKph = current.optDouble("wind_speed_10m", 0.0),
                    rainChancePercent = dailyFirstInt(daily, "precipitation_probability_max"),
                    minC = dailyFirstDouble(daily, "temperature_2m_min"),
                    maxC = dailyFirstDouble(daily, "temperature_2m_max"),
                    condition = describeWeatherCode(code, current.optInt("is_day", 1) == 1),
                    isDay = current.optInt("is_day", 1) == 1,
                    sunrise = dailyFirstString(daily, "sunrise"),
                    sunset = dailyFirstString(daily, "sunset")
                )
            } catch (e: Exception) {
                null
            }
        }

    private fun httpGet(url: String): String? {
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun dailyFirstDouble(daily: JSONObject?, key: String): Double {
        val array = daily?.optJSONArray(key) ?: return 0.0
        return if (array.length() > 0) array.optDouble(0, 0.0) else 0.0
    }

    private fun dailyFirstInt(daily: JSONObject?, key: String): Int {
        val array = daily?.optJSONArray(key) ?: return 0
        return if (array.length() > 0) array.optInt(0, 0) else 0
    }

    private fun dailyFirstString(daily: JSONObject?, key: String): String {
        val array = daily?.optJSONArray(key) ?: return ""
        return if (array.length() > 0) array.optString(0, "") else ""
    }

    companion object {
        /** WMO weather-code interpretation, spoken in Bengali. */
        fun describeWeatherCode(code: Int, isDay: Boolean): String = when (code) {
            0 -> if (isDay) "পরিষ্কার আকাশ" else "পরিষ্কার রাত"
            1 -> "প্রায় পরিষ্কার"
            2 -> "আংশিক মেঘলা"
            3 -> "মেঘলা আকাশ"
            45, 48 -> "কুয়াশা"
            51, 53, 55 -> "হালকা গুঁড়ি বৃষ্টি"
            56, 57 -> "জমাট বৃষ্টি"
            61 -> "হালকা বৃষ্টি"
            63 -> "মাঝারি বৃষ্টি"
            65 -> "ভারী বৃষ্টি"
            66, 67 -> "জমাট ভারী বৃষ্টি"
            71, 73, 75, 77 -> "তুষারপাত"
            80, 81, 82 -> "বৃষ্টির ঝাপটা"
            85, 86 -> "তুষারঝড়"
            95 -> "বজ্রসহ ঝড়"
            96, 99 -> "শিলাবৃষ্টিসহ বজ্রঝড়"
            else -> "আবহাওয়া স্বাভাবিক"
        }
    }
}
