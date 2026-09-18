package com.example.engine

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Fully offline calculator: understands Bengali word operators as well as symbolic expressions,
 * and is written as a small recursive-descent parser so no eval hack is needed.
 */
object CalculatorEngine {

    sealed class Result {
        data class Success(val value: Double, val expression: String) : Result()
        data class Failure(val message: String) : Result()
    }

    private val numberRegex = Regex("\\d+(?:\\.\\d+)?")

    private val wordOperatorMap = listOf(
        "যোগ" to "+",
        "যোগফল" to "+",
        "প্লাস" to "+",
        "sum" to "+",
        "plus" to "+",
        "এর সাথে" to "+",
        "বিয়োগ" to "-",
        "বিয়োগফল" to "-",
        "মাইনাস" to "-",
        "minus" to "-",
        "গুণ" to "*",
        "গুন" to "*",
        "গুণফল" to "*",
        "গুনফল" to "*",
        "times" to "*",
        "multiply" to "*",
        "ভাগ" to "/",
        "ভাগফল" to "/",
        "divide" to "/",
        "শতাংশ" to "%",
        "পার্সেন্ট" to "%",
        "percent" to "%",
        "বর্গমূল" to "sqrt"
    )

    /** Returns true when the text really looks like a calculation request (not casual chat). */
    fun looksLikeCalculation(text: String): Boolean {
        val normalized = TimePhraseParser.normalizeDigits(text).lowercase(Locale.ROOT)
        if (wordOperatorMap.any { normalized.contains(it.first) }) return true
        val hasOperator = normalized.contains(Regex("[+\\-*/^%]"))
        val hasMathFunction = normalized.contains("sqrt") || normalized.contains("বর্গমূল")
        val digits = numberRegex.findAll(normalized).count()
        return (hasOperator || hasMathFunction) && digits >= 1
    }

    fun calculate(rawInput: String): Result {
        val normalized = TimePhraseParser.normalizeDigits(rawInput).lowercase(Locale.ROOT)

        // 1. Word-operator handling ("২৫৬ এর সাথে ১২৫ যোগ করো")
        val wordOperator = wordOperatorMap.firstOrNull { normalized.contains(it.first) }
        if (wordOperator != null) {
            val numbers = numberRegex.findAll(normalized).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
            if (numbers.size >= 2) {
                val value = when (wordOperator.second) {
                    "+" -> numbers.sum()
                    "-" -> numbers.drop(1).fold(numbers.first()) { acc, n -> acc - n }
                    "*" -> numbers.fold(1.0) { acc, n -> acc * n }
                    "/" -> numbers.drop(1).fold(numbers.first()) { acc, n -> if (n == 0.0) Double.NaN else acc / n }
                    "%" -> numbers.first() * numbers.getOrElse(1) { 1.0 } / 100.0
                    else -> Double.NaN
                }
                if (!value.isNaN()) {
                    return Result.Success(value, "${numbers.joinToString(" ${wordOperator.second} ")} = ${format(value)}")
                }
                return Result.Failure("ভাগ শূন্য দিয়ে করা যায় না।")
            }
            if (wordOperator.second == "sqrt" && numbers.size == 1) {
                val value = sqrt(numbers.first())
                return Result.Success(value, "√${format(numbers.first())} = ${format(value)}")
            }
        }

        // 2. Symbolic expression evaluation
        val expression = normalized
            .replace("×", "*")
            .replace("÷", "/")
            .replace("x", "*")
            .replace("=", " ")
            .replace(Regex("[?।,!?]"), " ")
        val cleaned = expression.filter { it.isDigit() || it == '.' || it == '+' || it == '-' || it == '*' || it == '/' || it == '%' || it == '^' || it == '(' || it == ')' || it == ' ' }
        if (cleaned.isBlank() || numberRegex.find(cleaned) == null) {
            return Result.Failure("হিসাবের মতো কিছু পাইনি।")
        }
        return try {
            val parser = Parser(cleaned)
            val value = parser.parseExpression()
            if (value.isNaN() || value.isInfinite()) {
                Result.Failure("এই হিসাবটা করা যাচ্ছে না।")
            } else {
                Result.Success(value, "${cleaned.trim()} = ${format(value)}")
            }
        } catch (e: Exception) {
            Result.Failure("হিসাবটা বুঝতে পারিনি, একটু পরিষ্কার করে বলো?")
        }
    }

    fun format(value: Double): String {
        val rounded = if (abs(value) >= 1e12) value else (value * 1_000_000.0).roundToLong() / 1_000_000.0
        return if (abs(rounded - rounded.toLong()) < 0.0000001) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    fun formatBengali(value: Double): String = TimePhraseParser.toBengaliDigits(format(value))

    private class Parser(private val text: String) {
        private var position = 0

        fun parseExpression(): Double {
            var result = parseTerm()
            while (position < text.length) {
                val char = text[position]
                when (char) {
                    '+' -> {
                        position++
                        result += parseTerm()
                    }
                    '-' -> {
                        position++
                        result -= parseTerm()
                    }
                    else -> return result
                }
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parseFactor()
            while (position < text.length) {
                val char = text[position]
                when (char) {
                    '*' -> {
                        position++
                        result *= parseFactor()
                    }
                    '/' -> {
                        position++
                        val divisor = parseFactor()
                        result = if (divisor == 0.0) Double.NaN else result / divisor
                    }
                    '%' -> {
                        position++
                        val divisor = parseFactor()
                        result = if (divisor == 0.0) Double.NaN else result % divisor
                    }
                    else -> return result
                }
            }
            return result
        }

        private fun parseFactor(): Double {
            skipSpaces()
            if (position < text.length && text[position] == '(') {
                position++
                val value = parseExpression()
                skipSpaces()
                if (position < text.length && text[position] == ')') position++
                return applyPower(value)
            }
            if (position < text.length && (text[position] == '-' || text[position] == '+')) {
                val sign = if (text[position] == '-') -1.0 else 1.0
                position++
                return sign * parseFactor()
            }
            val start = position
            while (position < text.length && (text[position].isDigit() || text[position] == '.')) {
                position++
            }
            if (start == position) {
                position++
                return 0.0
            }
            val value = text.substring(start, position).toDoubleOrNull() ?: 0.0
            return applyPower(value)
        }

        private fun applyPower(base: Double): Double {
            skipSpaces()
            if (position < text.length && text[position] == '^') {
                position++
                return base.pow(parseFactor())
            }
            return base
        }

        private fun skipSpaces() {
            while (position < text.length && text[position] == ' ') position++
        }
    }
}
