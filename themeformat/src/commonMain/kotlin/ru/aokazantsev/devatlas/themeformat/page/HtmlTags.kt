package ru.aokazantsev.devatlas.themeformat.page

internal object HtmlTags {
    val headings = mapOf("h1" to 1, "h2" to 2, "h3" to 3, "h4" to 4, "h5" to 5, "h6" to 6)
    val blocks = headings.keys + setOf("p", "ul", "ol", "pre", "blockquote", "table", "figure", "hr", "img")
    val inlineStyles = setOf("b", "strong", "i", "em", "code")
    val wrappers = setOf("html", "head", "body")
    val dropped = setOf("script", "style", "template", "iframe", "object", "embed", "svg", "math", "noscript", "form", "button", "input")
    val tableSections = setOf("thead", "tbody", "tfoot")
    val tableCells = setOf("td", "th")

    val supported: Set<String> = blocks + inlineStyles + wrappers + tableSections +
        tableCells + setOf("li", "tr", "figcaption", "a", "br")

    val globalAttributes = setOf("id", "title", "lang", "dir", "class")
    val attributesByTag = mapOf(
        "a" to setOf("href", "download", "target", "rel"),
        "img" to setOf("src", "alt", "width", "height"),
        "ol" to setOf("start"),
    )
}
