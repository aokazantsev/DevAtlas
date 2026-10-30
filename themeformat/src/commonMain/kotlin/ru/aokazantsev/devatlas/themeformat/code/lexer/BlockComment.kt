package ru.aokazantsev.devatlas.themeformat.code.lexer

internal data class BlockComment(
    val open: String,
    val close: String,
    val nested: Boolean = false,
    val atLineStart: Boolean = false,
)
