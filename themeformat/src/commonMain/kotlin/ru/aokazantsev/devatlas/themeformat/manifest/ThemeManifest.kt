package ru.aokazantsev.devatlas.themeformat.manifest

data class ThemeManifest(
    val formatVersion: Int,
    val id: String,
    val title: String,
    val description: String?,
    val tree: List<ThemeNode>,
) {
    val pages: List<ThemeNode.Page>
        get() = buildList { collectPages(tree) }

    private fun MutableList<ThemeNode.Page>.collectPages(nodes: List<ThemeNode>) {
        nodes.forEach { node ->
            when (node) {
                is ThemeNode.Page -> add(node)
                is ThemeNode.Section -> collectPages(node.children)
            }
        }
    }

    companion object {
        const val FILE_NAME = "manifest.json"
        const val CURRENT_FORMAT_VERSION = 1
        const val MAX_TREE_DEPTH = 6
    }
}
