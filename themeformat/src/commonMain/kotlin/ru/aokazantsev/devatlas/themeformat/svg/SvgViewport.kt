package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.markup.MarkupAttribute
import ru.aokazantsev.devatlas.themeformat.markup.MarkupDialect
import ru.aokazantsev.devatlas.themeformat.markup.MarkupToken
import ru.aokazantsev.devatlas.themeformat.markup.MarkupTokenizer

data class SvgViewport(val left: Float, val top: Float, val width: Float, val height: Float) {

    val ratio: Float
        get() = width / height

    companion object {
        private const val DEFAULT_WIDTH = 300f
        private const val DEFAULT_HEIGHT = 150f
        private const val VIEW_BOX_PARTS = 4
        private const val ROOT_SEARCH_PREFIX = 4096
        private val separators = Regex("[\\s,]+")
        private val length = Regex("""\s*([+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?)\s*([a-zA-Z]*)\s*""")
        private val unitsInPixels = mapOf(
            "" to 1f,
            "px" to 1f,
            "pt" to 4f / 3f,
            "pc" to 16f,
            "in" to 96f,
            "cm" to 96f / 2.54f,
            "mm" to 96f / 25.4f,
        )

        fun of(svg: String): SvgViewport? {
            val root = rootTag(svg) ?: return null
            if (root.name.substringAfter(':') != "svg") return null
            val viewport = viewBox(root.attribute("viewBox")) ?: sizeBox(root)
            val ratio = viewport.width / viewport.height
            return viewport.takeIf { ratio.isFinite() && ratio > 0f }
        }

        private fun viewBox(value: String?): SvgViewport? {
            val parts = value?.trim()?.split(separators)?.mapNotNull(String::toFloatOrNull) ?: return null
            if (parts.size != VIEW_BOX_PARTS) return null
            val (left, top) = parts
            val (width, height) = parts.takeLast(2)
            return SvgViewport(left, top, width, height).takeIf { width > 0f && height > 0f }
        }

        private fun sizeBox(root: MarkupToken.StartTag): SvgViewport {
            val width = pixels(root.attribute("width"))?.takeIf { it > 0f }
            val height = pixels(root.attribute("height"))?.takeIf { it > 0f }
            return if (width != null && height != null) {
                SvgViewport(0f, 0f, width, height)
            } else {
                SvgViewport(0f, 0f, DEFAULT_WIDTH, DEFAULT_HEIGHT)
            }
        }

        private fun pixels(value: String?): Float? {
            val match = value?.let(length::matchEntire) ?: return null
            val number = match.groupValues[1].toFloatOrNull() ?: return null
            val unit = unitsInPixels[match.groupValues[2].lowercase()] ?: return null
            return number * unit
        }

        private fun MarkupToken.StartTag.attribute(name: String): String? =
            attributes.firstOrNull { it.name == name }?.let(MarkupAttribute::value)

        private fun rootTag(svg: String): MarkupToken.StartTag? {
            val dialect = MarkupDialect.Xml
            var length = ROOT_SEARCH_PREFIX
            while (true) {
                val (tokens, _) = MarkupTokenizer(svg.take(length), dialect.caseSensitive, dialect.rawTextElements).tokenize()
                tokens.firstNotNullOfOrNull { it as? MarkupToken.StartTag }?.let { return it }
                if (length >= svg.length) return null
                length *= 2
            }
        }
    }
}
