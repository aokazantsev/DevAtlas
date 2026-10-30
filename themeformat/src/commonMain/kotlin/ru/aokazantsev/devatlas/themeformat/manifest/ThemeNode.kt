package ru.aokazantsev.devatlas.themeformat.manifest

sealed interface ThemeNode {
    val id: String
    val title: String

    data class Section(
        override val id: String,
        override val title: String,
        val children: List<ThemeNode>,
    ) : ThemeNode

    data class Page(
        override val id: String,
        override val title: String,
        val file: String,
    ) : ThemeNode
}
