package ru.aokazantsev.devatlas.themeformat.code.lexer

internal fun LexerState.identifierEnd(from: Int, extra: String = ""): Int {
    var index = from
    while (index < end && LexerState.isIdentifierPart(text[index], extra)) index++
    return index
}

internal fun LexerState.skipSpaces(from: Int = pos): Int {
    var index = from
    while (index < end && (text[index] == ' ' || text[index] == '\t')) index++
    return index
}

internal fun LexerState.afterClosing(openIndex: Int, open: Char, close: Char, limit: Int = end): Int {
    var depth = 0
    var index = openIndex
    while (index < limit) {
        when (text[index]) {
            open -> depth++
            close -> depth--
        }
        index++
        if (depth == 0) return index
    }
    return limit
}

internal fun LexerState.skipWhitespace(from: Int): Int {
    var index = from
    while (index < end && text[index].isWhitespace()) index++
    return index
}
