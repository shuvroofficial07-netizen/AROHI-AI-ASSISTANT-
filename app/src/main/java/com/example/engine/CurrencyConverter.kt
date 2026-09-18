package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Currency conversion. Live rates come from the free open.er-api.com endpoint; a bundled
 * snapshot is used whenever the network is unavailable, so this always answers.
 */
object CurrencyConverter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /** Rates expressed as "how many units of X per 1 USD". */
    private val fallbackRates: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "BDT" to 122.0,
        "INR" to 87.5,
        "EUR" to 0.92,
        "GBP" to 0.78,
        "SAR" to 3.75,
        "AED" to 3.67,
        "MYR" to 4.35,
        "JPY" to 152.0,
        "PKR" to 278.0,
        "NPR" to 140.0,
        "KWD" to 0.31,
        "QAR" to 3.64,
        "OMR" to 0.385,
        "SGD" to 1.34,
        "AUD" to 1.52,
        "CAD" to 1.38,
        "CNY" to 7.25
    )

    private val keywords: Map<String, List<String>> = mapOf(
        "BDT" to listOf("টাকা", "taka", "bdt", "৳", "টাকায়"),
        "USD" to listOf("ডলার", "dollar", "usd", "us dollar", "$"),
        "EUR" to listOf("ইউরো", "euro", "eur"),
        "GBP" to listOf("পাউন্ড স্টার্লিং", "pound sterling", "gbp", "স্টার্লিং"),
        "INR" to listOf("ভারতীয় রুপি", "রুপি", "rupee", "inr", "rs"),
        "SAR" to listOf("সৌদি রিয়াল", "riyal", "sar"),
        "AED" to listOf("দিরহাম", "dirham", "aed"),
        "MYR" to listOf("রিঙ্গিত", "ringgit", "myr"),
        "JPY" to listOf("ইয়েন", "yen", "jpy"),
        "PKR" to listOf("পাকিস্তানি রুপি", "pkr"),
        "NPR" to listOf("নেপালি রুপি", "npr"),
        "KWD" to listOf("দিনার", "dinar", "kwd"),
        "QAR" to listOf("কাতারি রিয়াল", "qar"),
        "OMR" to listOf("ওমানি রিয়াল", "omr"),
        "SGD" to listOf("সিঙ্গাপুর ডলার", "sgd"),
        "AUD" to listOf("অস্ট্রেলিয়ান ডলার", "aud"),
        "CAD" to listOf("কানাডিয়ান ডলার", "cad"),
        "CNY" to listOf("ইউয়ান", "yuan", "cny", "renminbi")
    )

    @Volatile
    private var liveRates: Map<String, Double>? = null

    @Volatile
    private var liveFetchedAt: Long = 0L

    @Volatile
    var usedLiveRates: Boolean = false
        private set

    fun looksLikeCurrencyConversion(text: String): Boolean {
        val normalized = TimePhraseParser.normalizeDigits(text).lowercase(Locale.ROOT)
        if (!normalized.any { it.isDigit() }) return false
        val mentioned = resolveCurrencies(normalized)
        if (mentioned.size < 2) return false
        return normalized.contains("কত") || normalized.contains("থেকে") || normalized.contains("convert") ||
            normalized.contains("how much") || normalized.contains("in ") || normalized.contains("=")
    }

    fun resolveCurrencies(text: String): List<String> {
        val lower = text.lowercase(Locale.ROOT)
        return keywords.keys.filter { code ->
            keywords[code]?.any { lower.contains(it) } == true
        }
    }

    suspend fun currentRates(force: Boolean = false): Map<String, Double> = withContext(Dispatchers.IO) {
        val cached = liveRates
        if (!force && cached != null && System.currentTimeMillis() - liveFetchedAt < 30L * 60L * 1000L) {
            usedLiveRates = true
            return@withContext cached
        }
        val fetched = fetchLiveRates()
        if (fetched != null) {
            liveRates = fetched
            liveFetchedAt = System.currentTimeMillis()
            usedLiveRates = true
            fetched
        } else {
            usedLiveRates = false
            liveRates ?: fallbackRates
        }
    }

    private fun fetchLiveRates(): Map<String, Double>? {
        return try {
            val request = Request.Builder().url("https://open.er-api.com/v6/latest/USD").get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val ratesObject = JSONObject(body).optJSONObject("rates") ?: return null
                val parsed = mutableMapOf<String, Double>()
                for (code in keywords.keys) {
                    val value = ratesObject.optDouble(code, -1.0)
                    if (value > 0) parsed[code] = value
                }
                if (parsed.isEmpty()) null else parsed
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun convertFromText(text: String): String {
        val normalized = TimePhraseParser.normalizeDigits(text).lowercase(Locale.ROOT)
        val amount = Regex("(\\d+(?:\\.\\d+)?)").find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return "কত টাকা বা ডলার বদলাতে চাও সেটা বলো — যেমন '১০০ ডলার কত টাকা'।"

        val mentioned = resolveCurrencies(normalized)
        if (mentioned.size < 2) {
            return "কোন মুদ্রা থেকে কোনটায় বদলাতে হবে সেটা বলো — যেমন '১০০ ডলার কত টাকা'।"
        }
        val (before, after) = splitOnce(normalized)
        val beforeCurrencies = resolveCurrencies(before)
        val afterCurrencies = resolveCurrencies(after)
        val source = beforeCurrencies.firstOrNull() ?: mentioned.first()
        val target = afterCurrencies.firstOrNull { it != source } ?: mentioned.first { it != source }

        val rates = currentRates()
        val sourceRate = rates[source]
        val targetRate = rates[target]
        if (sourceRate == null || targetRate == null || sourceRate <= 0) {
            return "দুঃখিত, এই মুদ্রা দুটোর রেট এখন পাওয়া যাচ্ছে না।"
        }
        val usd = amount / sourceRate
        val result = usd * targetRate
        val rateNote = if (usedLiveRates) "লাইভ রেট" else "সাম্প্রতিক রেট (অনুমান)"
        val usdBase = rates["USD"] ?: 1.0
        val perUsd = if (usdBase > 0) targetRate / usdBase else targetRate
        return "${cleanAmount(amount)} ${codeLabel(source)} = ${cleanAmount(result)} ${codeLabel(target)} " +
            "($rateNote: ১ ডলার ≈ ${cleanAmount(perUsd)} ${codeLabel(target)})"
    }

    private fun splitOnce(text: String): Pair<String, String> {
        val markers = listOf("থেকে", " to ", " into ", " in ", "=", "→")
        for (marker in markers) {
            val index = text.indexOf(marker)
            if (index >= 0) {
                return Pair(text.substring(0, index), text.substring(index + marker.length))
            }
        }
        return Pair(text, text)
    }

    private fun codeLabel(code: String): String = when (code) {
        "BDT" -> "বাংলাদেশি টাকা"
        "USD" -> "মার্কিন ডলার"
        "EUR" -> "ইউরো"
        "GBP" -> "ব্রিটিশ পাউন্ড"
        "INR" -> "ভারতীয় রুপি"
        "SAR" -> "সৌদি রিয়াল"
        "AED" -> "সংযুক্ত আরব আমিরাতি দিরহাম"
        "MYR" -> "মালয়েশিয়ান রিঙ্গিত"
        "JPY" -> "জাপানি ইয়েন"
        "PKR" -> "পাকিস্তানি রুপি"
        "NPR" -> "নেপালি রুপি"
        "KWD" -> "কুয়েতি দিনার"
        "QAR" -> "কাতারি রিয়াল"
        "OMR" -> "ওমানি রিয়াল"
        "SGD" -> "সিঙ্গাপুর ডলার"
        "AUD" -> "অস্ট্রেলিয়ান ডলার"
        "CAD" -> "কানাডিয়ান ডলার"
        "CNY" -> "চীনা ইউয়ান"
        else -> code
    }

    private fun cleanAmount(value: Double): String {
        val rounded = (value * 100).toLong() / 100.0
        val text = if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else String.format(Locale.US, "%.2f", rounded)
        return TimePhraseParser.toBengaliDigits(text)
    }
}
