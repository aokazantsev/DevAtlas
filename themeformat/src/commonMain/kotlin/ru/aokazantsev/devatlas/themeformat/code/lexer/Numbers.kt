package ru.aokazantsev.devatlas.themeformat.code.lexer

private const val RADIX_MARKERS = "xbo"

internal fun LexerState.numberEnd(from: Int, quoteSeparator: Boolean = false): Int {
    if (charAt(from) == '0' && charAt(from + 1)?.lowercaseChar()?.let { it in RADIX_MARKERS } == true) {
        return wordTail(from + 2, quoteSeparator)
    }
    var index = digits(from, quoteSeparator)
    if (charAt(index) == '.' && charAt(index + 1)?.isDigit() == true) index = digits(index + 1, quoteSeparator)
    if (charAt(index)?.lowercaseChar() == 'e') {
        val sign = charAt(index + 1)
        val exponent = if (sign == '+' || sign == '-') index + 2 else index + 1
        if (charAt(exponent)?.isDigit() == true) index = digits(exponent, quoteSeparator)
    }
    return wordTail(index, quoteSeparator)
}

private fun LexerState.digits(from: Int, quoteSeparator: Boolean): Int {
    var index = from
    while (charAt(index)?.let { it.isDigit() || isSeparator(index, quoteSeparator) } == true) index++
    return index
}

private fun LexerState.wordTail(from: Int, quoteSeparator: Boolean): Int {
    var index = from
    while (charAt(index)?.let { it.isLetterOrDigit() || isSeparator(index, quoteSeparator) } == true) index++
    return index
}

private fun LexerState.isSeparator(index: Int, quoteSeparator: Boolean): Boolean =
    charAt(index) == '_' || quoteSeparator && charAt(index) == '\'' && charAt(index + 1)?.isLetterOrDigit() == true
