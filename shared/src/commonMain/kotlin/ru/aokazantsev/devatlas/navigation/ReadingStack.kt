package ru.aokazantsev.devatlas.navigation

import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

fun readingStack(tree: List<ThemeNode>, target: ScreenTarget, anchor: Int): List<Route>? {
    val path = when (target) {
        ScreenTarget.Root -> return listOf(Route.Contents(anchor))
        is ScreenTarget.Section -> tree.pathTo { it is ThemeNode.Section && it.id == target.id }
        is ScreenTarget.Page -> tree.pathTo { it is ThemeNode.Page && it.id == target.id }
    } ?: return null
    return buildList {
        add(Route.Contents(path.first().index))
        path.zipWithNext { parent, child -> add(Route.Section(parent.value.id, child.index)) }
        add(
            when (target) {
                is ScreenTarget.Section -> Route.Section(target.id, anchor)
                else -> Route.Page(path.last().value.id, anchor)
            },
        )
    }
}

private fun List<ThemeNode>.pathTo(matches: (ThemeNode) -> Boolean): List<IndexedValue<ThemeNode>>? {
    forEachIndexed { index, node ->
        val step = IndexedValue(index, node)
        if (matches(node)) return listOf(step)
        if (node is ThemeNode.Section) node.children.pathTo(matches)?.let { return listOf(step) + it }
    }
    return null
}
