package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.markup.MarkupNode

internal class SvgElementCounter(
    private val ids: Map<String, MarkupNode.Element>,
    private val limit: Long,
    private val maxDepth: Int,
) {
    private val counted = mutableMapOf<String, Long>()
    private val chainDepths = mutableMapOf<String, Int>()
    private val resolving = mutableSetOf<String>()
    private var deepest = 0
    var overflow = false
        private set

    fun count(element: MarkupNode.Element, depth: Int): Long {
        if (overflow) return 0
        var total = 1L
        element.children.forEach { child ->
            if (child is MarkupNode.Element) total = capped(total + count(child, depth))
        }
        if (element.name.substringAfter(':') == "use") total = capped(total + referenced(element, depth))
        return total
    }

    private fun referenced(use: MarkupNode.Element, depth: Int): Long {
        val target = use.attributes.firstOrNull { it.name.substringAfter(':') == "href" }?.value?.trim()
            ?.takeIf { it.startsWith("#") }?.drop(1)
        val element = target?.let { ids[it] } ?: return 0
        val known = counted[target] ?: return expand(target, element, depth)
        reach(depth + 1 + chainDepths.getValue(target))
        return known
    }

    private fun expand(target: String, element: MarkupNode.Element, depth: Int): Long {
        if (target in resolving) {
            overflow = true
            return 0
        }
        val outer = deepest
        deepest = depth + 1
        reach(deepest)
        resolving += target
        val total = count(element, depth + 1)
        resolving -= target
        counted[target] = total
        chainDepths[target] = deepest - (depth + 1)
        deepest = maxOf(outer, deepest)
        return total
    }

    private fun reach(depth: Int) {
        if (depth > maxDepth) overflow = true
        deepest = maxOf(deepest, depth)
    }

    private fun capped(value: Long): Long {
        if (value > limit) overflow = true
        return value.coerceAtMost(limit + 1)
    }
}
