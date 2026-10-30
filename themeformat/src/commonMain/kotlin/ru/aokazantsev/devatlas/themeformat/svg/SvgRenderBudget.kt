package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.markup.MarkupDialect
import ru.aokazantsev.devatlas.themeformat.markup.MarkupNode

object SvgRenderBudget {

    private const val MAX_RENDERED_ELEMENTS = 20_000L
    private const val MAX_REFERENCE_DEPTH = 32

    fun fits(source: String): Boolean {
        val (nodes, problems) = MarkupDialect.Xml.parse(source)
        if (problems.isNotEmpty()) return false
        val ids = mutableMapOf<String, MarkupNode.Element>()
        nodes.forEach { collectIds(it, ids) }
        val counter = SvgElementCounter(ids, MAX_RENDERED_ELEMENTS, MAX_REFERENCE_DEPTH)
        val total = nodes.filterIsInstance<MarkupNode.Element>().sumOf { counter.count(it, depth = 0) }
        return !counter.overflow && total <= MAX_RENDERED_ELEMENTS
    }

    private fun collectIds(node: MarkupNode, into: MutableMap<String, MarkupNode.Element>) {
        if (node !is MarkupNode.Element) return
        (node.attribute("id") ?: node.attribute("xml:id"))?.value?.let { into.putIfAbsent(it, node) }
        node.children.forEach { collectIds(it, into) }
    }
}
