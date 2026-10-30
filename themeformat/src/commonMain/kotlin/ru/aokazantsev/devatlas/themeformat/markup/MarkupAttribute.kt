package ru.aokazantsev.devatlas.themeformat.markup

data class MarkupAttribute(
    val name: String,
    val value: String?,
    val position: SourcePosition,
)
