package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.markup.MarkupDialect
import ru.aokazantsev.devatlas.themeformat.markup.MarkupToken
import ru.aokazantsev.devatlas.themeformat.markup.MarkupTokenizer

object SvgValueRewriter {

    fun rewrite(svg: String, transform: (String) -> String): String {
        val dialect = MarkupDialect.Xml
        val (tokens, _) = MarkupTokenizer(svg, dialect.caseSensitive, dialect.rawTextElements).tokenize()
        val ranges = valueRanges(tokens)
        val result = StringBuilder(svg.length)
        var cursor = 0
        ranges.forEach { range ->
            result.append(svg, cursor, range.first)
            result.append(transform(svg.substring(range.first, range.last + 1)))
            cursor = range.last + 1
        }
        result.append(svg, cursor, svg.length)
        return result.toString()
    }

    private fun valueRanges(tokens: List<MarkupToken>): List<IntRange> =
        tokens.filterIsInstance<MarkupToken.StartTag>().flatMap { tag -> tag.attributes.mapNotNull { it.valueRange } }
}
