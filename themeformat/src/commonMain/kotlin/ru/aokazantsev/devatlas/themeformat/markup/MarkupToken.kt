package ru.aokazantsev.devatlas.themeformat.markup

sealed interface MarkupToken {
    val position: SourcePosition

    data class StartTag(
        val name: String,
        val attributes: List<MarkupAttribute>,
        val selfClosing: Boolean,
        override val position: SourcePosition,
    ) : MarkupToken

    data class EndTag(val name: String, override val position: SourcePosition) : MarkupToken

    data class Text(val text: String, override val position: SourcePosition) : MarkupToken
}
