package ru.aokazantsev.devatlas.themeformat.markup

internal object Entities {

    private const val MAX_ENTITY_LENGTH = 12
    private const val HEX_RADIX = 16
    private const val DECIMAL_RADIX = 10
    private const val MAX_CODE_POINT = 0x10FFFF
    private const val SUPPLEMENTARY_START = 0x10000
    private const val HIGH_SURROGATE_START = 0xD800
    private const val LOW_SURROGATE_START = 0xDC00
    private const val SURROGATE_BITS = 10
    private const val SURROGATE_MASK = 0x3FF

    private val named = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to "\u00A0",
        "shy" to "\u00AD",
        "mdash" to "—",
        "ndash" to "–",
        "minus" to "−",
        "laquo" to "«",
        "raquo" to "»",
        "ldquo" to "“",
        "rdquo" to "”",
        "bdquo" to "„",
        "lsquo" to "‘",
        "rsquo" to "’",
        "hellip" to "…",
        "copy" to "©",
        "reg" to "®",
        "trade" to "™",
        "times" to "×",
        "divide" to "÷",
        "deg" to "°",
        "plusmn" to "±",
        "le" to "≤",
        "ge" to "≥",
        "ne" to "≠",
        "larr" to "←",
        "rarr" to "→",
        "uarr" to "↑",
        "darr" to "↓",
        "harr" to "↔",
        "rArr" to "⇒",
        "middot" to "·",
        "bull" to "•",
        "sect" to "§",
        "euro" to "€",
    )

    private val namedReference = Regex("&([A-Za-z][A-Za-z0-9]{0,31});")

    private val xmlInvalidReference = Regex("&(?!(?:amp|lt|gt|quot|apos|#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6});)([A-Za-z][A-Za-z0-9]{0,31};)?")

    fun decode(text: String): String {
        if ('&' !in text) return text
        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index]
            val end = if (char == '&') semicolonAfter(text, index) else -1
            val replacement = if (end > index) resolve(text.substring(index + 1, end)) else null
            if (replacement != null) {
                result.append(replacement)
                index = end + 1
            } else {
                result.append(char)
                index++
            }
        }
        return result.toString()
    }

    fun undefinedInXml(text: String): MatchResult? = if ('&' in text) xmlInvalidReference.find(text) else null

    fun unknownNamed(text: String): MatchResult? =
        if ('&' in text) namedReference.findAll(text).firstOrNull { it.groupValues[1] !in named } else null

    private fun semicolonAfter(text: String, ampersand: Int): Int {
        val last = minOf(text.length - 1, ampersand + MAX_ENTITY_LENGTH)
        for (candidate in ampersand + 1..last) {
            if (text[candidate] == ';') return candidate
        }
        return -1
    }

    private fun resolve(body: String): String? {
        if (!body.startsWith("#")) return named[body]
        val code = if (body.startsWith("#x") || body.startsWith("#X")) {
            body.drop(2).toIntOrNull(HEX_RADIX)
        } else {
            body.drop(1).toIntOrNull(DECIMAL_RADIX)
        }
        return code?.takeIf { it in 1..MAX_CODE_POINT }?.let(::codePointToString)
    }

    private fun codePointToString(codePoint: Int): String {
        if (codePoint < SUPPLEMENTARY_START) return codePoint.toChar().toString()
        val offset = codePoint - SUPPLEMENTARY_START
        val high = (HIGH_SURROGATE_START + (offset shr SURROGATE_BITS)).toChar()
        val low = (LOW_SURROGATE_START + (offset and SURROGATE_MASK)).toChar()
        return "$high$low"
    }
}
