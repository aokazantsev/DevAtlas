package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal data class StringRule(
    val open: String,
    val close: String = open,
    val escapes: Boolean = true,
    val multiline: Boolean = false,
    val templates: StringTemplates = StringTemplates.NONE,
    val kind: CodeTokenKind? = CodeTokenKind.STRING,
)
