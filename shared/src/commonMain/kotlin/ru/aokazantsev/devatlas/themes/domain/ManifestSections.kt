package ru.aokazantsev.devatlas.themes.domain

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

internal fun ThemeManifest.sectionIds(): List<String> = tree.sectionIds()

private fun List<ThemeNode>.sectionIds(): List<String> = flatMap { node ->
    if (node is ThemeNode.Section) listOf(node.id) + node.children.sectionIds() else emptyList()
}
