package ru.aokazantsev.devatlas.themeformat.markup

internal sealed interface MarkupDialect {
    val caseSensitive: Boolean
    val rawTextElements: Set<String>
    val voidElements: Set<String>
    val optionalEnd: Set<String>

    fun impliedEnds(startTag: String): List<ImpliedEnd>

    fun parse(source: String): Pair<List<MarkupNode>, List<MarkupProblem>> {
        val (tokens, tokenProblems) = MarkupTokenizer(source, caseSensitive, rawTextElements).tokenize()
        val (nodes, treeProblems) = MarkupTreeBuilder(this).build(tokens)
        return nodes to (tokenProblems + treeProblems).sortedWith(compareBy({ it.position.line }, { it.position.column }))
    }

    data object Html : MarkupDialect {
        override val caseSensitive = false
        override val rawTextElements = setOf("script", "style")
        override val voidElements = setOf("br", "hr", "img", "meta", "link", "input", "wbr", "col", "source", "base")
        override val optionalEnd = setOf("p", "li", "td", "th", "tr", "thead", "tbody", "tfoot", "html", "head", "body")

        private val paragraphClosers = setOf(
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "pre", "table", "blockquote", "figure", "hr", "div",
        )
        private val closeParagraph = ImpliedEnd(targets = setOf("p"), boundaries = setOf("li", "td", "th", "blockquote", "figure"))
        private val closeItem = ImpliedEnd(targets = setOf("li"), boundaries = setOf("ul", "ol"))
        private val closeCell = ImpliedEnd(targets = setOf("td", "th"), boundaries = setOf("tr", "table"))
        private val closeRow = ImpliedEnd(targets = setOf("tr"), boundaries = setOf("table"))
        private val closeSection = ImpliedEnd(targets = setOf("thead", "tbody", "tfoot"), boundaries = setOf("table"))

        override fun impliedEnds(startTag: String): List<ImpliedEnd> = buildList {
            if (startTag in paragraphClosers) add(closeParagraph)
            when (startTag) {
                "li" -> add(closeItem)
                "td", "th" -> add(closeCell)
                "tr" -> add(closeRow)
                "thead", "tbody", "tfoot" -> add(closeSection)
            }
        }
    }

    data object Xml : MarkupDialect {
        override val caseSensitive = true
        override val rawTextElements = setOf("script", "style")
        override val voidElements = emptySet<String>()
        override val optionalEnd = emptySet<String>()

        override fun impliedEnds(startTag: String): List<ImpliedEnd> = emptyList()
    }
}
