package ru.aokazantsev.devatlas.themeformat.page.model

data class InlineStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
) {
    companion object {
        val Plain = InlineStyle()
    }
}
