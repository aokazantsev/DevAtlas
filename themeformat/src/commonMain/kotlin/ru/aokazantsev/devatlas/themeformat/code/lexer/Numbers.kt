package ru.aokazantsev.devatlas.themeformat.code.lexer

private const val RADIX_MARKERS = "xbo"

internal fun LexerState.numberEnd(from: Int): Int {
    if (charAt(from) == '0' && charAt(from + 1)?.lowercaseChar()?.let { it in RADIX_MARKERS } == true) {
        return wordTail(from + 2)
    }
    var index = digits(from)
    if (charAt(index) == '.' && charAt(index + 1)?.isDigit() == true) index = digits(index + 1)
    if (charAt(index)?.lowercaseChar() == 'e') {
        val sign = charAt(index + 1)
        val exponent = if (sign == '+' || sign == '-') index + 2 else index + 1
        if (charAt(exponent)?.isDigit() == true) index = digits(exponent)
    }
    return wordTail(index)
}

private fun LexerState.digits(from: Int): Int {
    var index = from
    while (charAt(index)?.let { it.isDigit() || it == '_' } == true) index++
    return index
}

private fun LexerState.wordTail(from: Int): Int {
    var index = from
    while (charAt(index)?.let { it.isLetterOrDigit() || it == '_' } == true) index++
    return index
}
