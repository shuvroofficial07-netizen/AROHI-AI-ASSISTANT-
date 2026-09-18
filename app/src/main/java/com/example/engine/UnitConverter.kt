package com.example.engine

import java.util.Locale

/**
 * Offline unit conversion with Bengali unit vocabulary (কেজি, মাইল, লিটার, গিগাবাইট ...).
 */
object UnitConverter {

    data class Unit(
        val id: String,
        val latin: List<String>,
        val bengali: String,
        val category: String,
        val factor: Double
    )

    private val units = listOf(
        // Length (base unit: metre)
        Unit("m", listOf("m", "meter", "metre"), "মিটার", "length", 1.0),
        Unit("km", listOf("km", "kilometer", "kilometre"), "কিলোমিটার", "length", 1000.0),
        Unit("cm", listOf("cm", "centimeter"), "সেন্টিমিটার", "length", 0.01),
        Unit("mm", listOf("mm", "millimeter"), "মিলিমিটার", "length", 0.001),
        Unit("mi", listOf("mi", "mile", "miles"), "মাইল", "length", 1609.344),
        Unit("ft", listOf("ft", "foot", "feet"), "ফুট", "length", 0.3048),
        Unit("inch", listOf("inch", "inches"), "ইঞ্চি", "length", 0.0254),
        Unit("yd", listOf("yd", "yard"), "গজ", "length", 0.9144),

        // Mass (base unit: kilogram)
        Unit("kg", listOf("kg", "kilo", "kilogram"), "কেজি", "mass", 1.0),
        Unit("g", listOf("g", "gram", "grams"), "গ্রাম", "mass", 0.001),
        Unit("mg", listOf("mg", "milligram"), "মিলিগ্রাম", "mass", 0.000001),
        Unit("lb", listOf("lb", "lbs", "pound", "pounds"), "পাউন্ড", "mass", 0.45359237),
        Unit("oz", listOf("oz", "ounce"), "আউন্স", "mass", 0.0283495),
        Unit("ton", listOf("ton", "tons", "tonne"), "টন", "mass", 1000.0),
        Unit("mon", listOf("maund"), "মণ", "mass", 37.3242),

        // Volume (base unit: litre)
        Unit("l", listOf("l", "liter", "litre"), "লিটার", "volume", 1.0),
        Unit("ml", listOf("ml", "milliliter"), "মিলিলিটার", "volume", 0.001),
        Unit("gal", listOf("gal", "gallon"), "গ্যালন", "volume", 3.78541),

        // Data (base unit: megabyte)
        Unit("gb", listOf("gb", "gigabyte"), "গিগাবাইট", "data", 1024.0),
        Unit("mb", listOf("mb", "megabyte"), "মেগাবাইট", "data", 1.0),
        Unit("kb", listOf("kb", "kilobyte"), "কিলোবাইট", "data", 1.0 / 1024.0),
        Unit("tb", listOf("tb", "terabyte"), "টেরাবাইট", "data", 1024.0 * 1024.0),

        // Speed (base unit: km/h)
        Unit("kmh", listOf("kmh", "kph"), "কিলোমিটার প্রতি ঘন্টা", "speed", 1.0),
        Unit("mph", listOf("mph"), "মাইল প্রতি ঘন্টা", "speed", 1.609344),
        Unit("mps", listOf("mps"), "মিটার প্রতি সেকেন্ড", "speed", 3.6)
    )

    private val temperatureKeywords = listOf(
        "সেলসিয়াস", "সেন্টিগ্রেড", "celsius",
        "ফারেনহাইট", "fahrenheit",
        "কেলভিন", "kelvin"
    )

    private fun mentionsTemperature(text: String): Boolean = temperatureKeywords.any { text.contains(it) }

    private fun mentionsUnit(text: String, unit: Unit): Boolean {
        if (text.contains(unit.bengali)) return true
        return unit.latin.any { token ->
            Regex("(^|[^a-zA-Z])" + Regex.escape(token) + "([^a-zA-Z]|$)").containsMatchIn(text)
        }
    }

    private fun unitsMentioned(text: String): List<Unit> = units.filter { mentionsUnit(text, it) }

    /** Heuristic gate so casual chat never reaches the converter. */
    fun looksLikeConversion(text: String): Boolean {
        val normalized = TimePhraseParser.normalizeDigits(text).lowercase(Locale.ROOT)
        if (mentionsTemperature(normalized) && normalized.any { it.isDigit() }) return true
        val mentioned = unitsMentioned(normalized)
        if (mentioned.size < 2) return false
        return normalized.contains("থেকে") || normalized.contains(" to ") || normalized.contains(" into ") ||
            normalized.contains("কত") || normalized.contains("convert") || normalized.contains("how much") ||
            normalized.contains("=")
    }

    fun convertFromText(text: String): String {
        val normalized = TimePhraseParser.normalizeDigits(text).lowercase(Locale.ROOT)
        val value = Regex("(-?\\d+(?:\\.\\d+)?)").find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return "কত পরিমাণ বদলাতে হবে সেটা বলো — যেমন '২০ কেজি কত পাউন্ড'।"

        val (before, after) = splitOnce(normalized)
        val unitsBefore = unitsMentioned(before)
        val unitsAfter = unitsMentioned(after)
        val allUnits = unitsMentioned(normalized)

        // 1. Temperature
        if (mentionsTemperature(normalized)) {
            val source = detectTemperature(before) ?: detectTemperature(normalized)
            val target = detectTemperature(after) ?: listOf("celsius", "fahrenheit", "kelvin")
                .firstOrNull { it != source && normalized.contains(it) }
            if (source == null || target == null || source == target) {
                return "কোন একক থেকে কোনটায় বদলাতে হবে সেটা পরিষ্কার করে বলো — যেমন '৩০ সেলসিয়াস কত ফারেনহাইট'।"
            }
            val celsius = when (source) {
                "celsius" -> value
                "fahrenheit" -> (value - 32) * 5 / 9
                else -> value - 273.15
            }
            val result = when (target) {
                "fahrenheit" -> celsius * 9 / 5 + 32
                "kelvin" -> celsius + 273.15
                else -> celsius
            }
            return "${cleanNumber(value)} ${temperatureLabel(source)} = ${cleanNumber(result)} ${temperatureLabel(target)}"
        }

        // 2. Regular units
        val source = unitsBefore.firstOrNull() ?: allUnits.firstOrNull()
            ?: return "কোন একক থেকে বদলাতে হবে সেটা বুঝতে পারিনি।"
        val target = unitsAfter.firstOrNull { it.id != source.id }
            ?: allUnits.firstOrNull { it.id != source.id }
            ?: return "কোন এককে বদলাতে হবে সেটা বলো।"

        if (source.category != target.category) {
            return "'${source.bengali}' থেকে '${target.bengali}' সরাসরি বদলানো যায় না — দুটো আলাদা ধরনের একক।"
        }
        val converted = (value * source.factor) / target.factor
        return "${cleanNumber(value)} ${source.bengali} = ${cleanNumber(converted)} ${target.bengali}"
    }

    private fun detectTemperature(text: String): String? = when {
        text.contains("সেলসিয়াস") || text.contains("সেন্টিগ্রেড") || text.contains("celsius") -> "celsius"
        text.contains("ফারেনহাইট") || text.contains("fahrenheit") -> "fahrenheit"
        text.contains("কেলভিন") || text.contains("kelvin") -> "kelvin"
        else -> null
    }

    private fun temperatureLabel(id: String): String = when (id) {
        "celsius" -> "ডিগ্রি সেলসিয়াস"
        "fahrenheit" -> "ডিগ্রি ফারেনহাইট"
        else -> "কেলভিন"
    }

    /** Splits "২০ কেজি থেকে পাউন্ড" into ("২০ কেজি", "পাউন্ড"). */
    private fun splitOnce(text: String): Pair<String, String> {
        val markers = listOf("থেকে", " to ", " into ", "→", "=")
        for (marker in markers) {
            val index = text.indexOf(marker)
            if (index >= 0) {
                return Pair(text.substring(0, index), text.substring(index + marker.length))
            }
        }
        return Pair(text, text)
    }

    private fun cleanNumber(value: Double): String {
        val rounded = (value * 10000).toLong() / 10000.0
        val text = if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
        }
        return TimePhraseParser.toBengaliDigits(text)
    }
}
