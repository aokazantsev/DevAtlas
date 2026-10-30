package ru.aokazantsev.devatlas.ui.page.diagram

object SvgColorTokens {

    private val variable = Regex("""var\(\s*--([A-Za-z0-9-]+)\s*\)""")
    private val currentColor = Regex("currentcolor", RegexOption.IGNORE_CASE)

    fun resolve(svg: String, palette: Map<String, String>, textColor: String): String =
        variable.replace(svg) { match -> palette[match.groupValues[1]] ?: textColor }
            .replace(currentColor, textColor)
}
