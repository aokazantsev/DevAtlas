package ru.aokazantsev.devatlas.themeformat.markup

sealed interface MarkupNode {
    val position: SourcePosition

    data class Element(
        val name: String,
        val attributes: List<MarkupAttribute>,
        val children: List<MarkupNode>,
        override val position: SourcePosition,
    ) : MarkupNode {
        fun attribute(name: String): MarkupAttribute? = attributes.firstOrNull { it.name == name }

        fun textContent(): String = buildString { appendText(this@Element) }

        private fun StringBuilder.appendText(node: MarkupNode) {
            when (node) {
                is Text -> append(node.text)
                is Element -> node.children.forEach { appendText(it) }
            }
        }
    }

    data class Text(val text: String, override val position: SourcePosition) : MarkupNode
}
