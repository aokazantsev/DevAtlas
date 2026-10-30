package ru.aokazantsev.devatlas.themeformat.page

import ru.aokazantsev.devatlas.themeformat.markup.MarkupDialect

object HtmlPageParser {

    fun parse(file: String, source: String): ParsedPage {
        val (nodes, problems) = MarkupDialect.Html.parse(source)
        val builder = HtmlPageBuilder(file)
        problems.forEach(builder::markupProblem)
        val document = builder.build(nodes)
        return ParsedPage(
            document = document,
            issues = builder.issues,
            pageReferences = builder.pageReferences,
            imageReferences = builder.imageReferences,
        )
    }
}
