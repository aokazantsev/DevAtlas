package ru.aokazantsev.devatlas.themeformat.page.model

sealed interface PageBlock {
    data class Heading(val level: Int, val content: List<PageInline>) : PageBlock

    data class Paragraph(val content: List<PageInline>) : PageBlock

    data class ListBlock(val ordered: Boolean, val items: List<List<PageBlock>>) : PageBlock

    data class Code(val code: String, val language: String?) : PageBlock

    data class Quote(val blocks: List<PageBlock>) : PageBlock

    data class Table(val rows: List<TableRow>) : PageBlock

    data class Figure(val source: String, val alt: String?, val caption: List<PageInline>) : PageBlock

    data object Divider : PageBlock
}
