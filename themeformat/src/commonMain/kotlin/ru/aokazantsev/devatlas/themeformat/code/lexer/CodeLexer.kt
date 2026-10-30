package ru.aokazantsev.devatlas.themeformat.code.lexer

internal fun interface CodeLexer {
    fun tokenize(state: LexerState)
}
