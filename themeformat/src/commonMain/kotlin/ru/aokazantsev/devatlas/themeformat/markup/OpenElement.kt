package ru.aokazantsev.devatlas.themeformat.markup

internal class OpenElement(val token: MarkupToken.StartTag) {
    val children = mutableListOf<MarkupNode>()

    fun toNode() = MarkupNode.Element(token.name, token.attributes, children.toList(), token.position)
}
