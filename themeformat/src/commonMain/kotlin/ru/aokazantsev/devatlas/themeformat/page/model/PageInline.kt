package ru.aokazantsev.devatlas.themeformat.page.model

sealed interface PageInline {
    data class Text(val text: String, val style: InlineStyle = InlineStyle.Plain) : PageInline

    data class Link(val target: LinkTarget, val download: String?, val content: List<PageInline>) : PageInline

    data object LineBreak : PageInline
}
