package ru.aokazantsev.devatlas.e2e

import org.json.JSONArray
import org.json.JSONObject

object TallDiagramTheme {

    const val ID = "tall-diagrams"
    const val TITLE = "Высокие схемы"
    const val PAGE_ID = "tall"
    const val PAGE_TITLE = "Схемы и абзацы"
    const val DIAGRAMS = 3
    const val PARAGRAPHS = 40
    const val LONG_PAGE_ID = "long"
    const val LONG_PAGE_TITLE = "Длинный абзац"
    const val LONG_BLOCK_START = "Длинный блок."
    const val LONG_PAGE_LINK = "к схемам и абзацам"

    private const val TALL_SVG =
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 300" fill="currentColor">""" +
            """<rect x="10" y="10" width="80" height="280" rx="8" fill="var(--primary-container)"/></svg>"""

    private val filler = "Строки абзаца переносятся по-разному в книжной и альбомной ориентации экрана. ".repeat(4)

    private val longFiller = "Этот абзац выше экрана, верх экрана оказывается внутри него. ".repeat(80)

    fun paragraphLabel(number: Int): String = "Абзац ${number.toString().padStart(2, '0')}."

    fun paragraphOfBlock(block: Int): Int = block - DIAGRAMS + 1

    fun zip(): ByteArray {
        val page = buildString {
            repeat(DIAGRAMS) { index -> append("<figure><img src=\"../diagrams/tall.svg\" alt=\"Высокая схема ${index + 1}\"></figure>\n") }
            (1..PARAGRAPHS).forEach { number -> append("<p>${paragraphLabel(number)} $filler</p>\n") }
        }
        val manifest = JSONObject()
            .put("formatVersion", 1)
            .put("id", ID)
            .put("title", TITLE)
            .put(
                "tree",
                JSONArray()
                    .put(JSONObject().put("id", PAGE_ID).put("title", PAGE_TITLE).put("file", "pages/tall.html"))
                    .put(JSONObject().put("id", LONG_PAGE_ID).put("title", LONG_PAGE_TITLE).put("file", "pages/long.html")),
            )
        val longPage = "<p>Начало страницы.</p>\n<p>$LONG_BLOCK_START $longFiller</p>\n" +
            "<p>Ссылка: <a href=\"page:$PAGE_ID\">$LONG_PAGE_LINK</a>.</p>\n"
        return ThemeZips.zip(
            linkedMapOf(
                ThemeZips.MANIFEST to manifest.toString().toByteArray(),
                "pages/tall.html" to page.toByteArray(),
                "pages/long.html" to longPage.toByteArray(),
                "diagrams/tall.svg" to TALL_SVG.toByteArray(),
            ),
        )
    }
}
