package ru.aokazantsev.devatlas.ui.page.code

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightedCodeTest {

    private val palette = CodePalette(
        keyword = Color.Red,
        string = Color.Green,
        number = Color.Blue,
        type = Color.Yellow,
        comment = Color.Gray,
    )

    private fun colorsOf(code: String, language: CodeLanguage?): Map<String, Color> =
        highlightedCode(code, codeSpans(code, language), palette).spanStyles.associate { code.substring(it.start, it.end) to it.item.color }

    @Test
    fun tokensGetRoleColors() {
        val colors = colorsOf("val name: String = \"x\" + 1 // c", CodeLanguage.KOTLIN)

        assertEquals(Color.Red, colors["val"])
        assertEquals(Color.Yellow, colors["String"])
        assertEquals(Color.Green, colors["\"x\""])
        assertEquals(Color.Blue, colors["1"])
        assertEquals(Color.Gray, colors["// c"])
    }

    @Test
    fun diffMarkupAndHeadingKindsGetTheirColors() {
        val diff = colorsOf("-old\n+new", CodeLanguage.DIFF)
        assertEquals(Color.Blue, diff["-old"])
        assertEquals(Color.Green, diff["+new"])
        val html = colorsOf("<p class=\"a\">", CodeLanguage.HTML)
        assertEquals(Color.Red, html["<p"])
        assertEquals(Color.Yellow, html["class"])
        val heading = highlightedCode("# T", codeSpans("# T", CodeLanguage.MARKDOWN), palette).spanStyles.single().item
        assertEquals(FontWeight.SemiBold, heading.fontWeight)
        assertEquals(Color.Red, heading.color)
    }

    @Test
    fun commentsAreItalic() {
        val style = highlightedCode("# note", codeSpans("# note", CodeLanguage.PYTHON), palette).spanStyles.single().item
        assertEquals(FontStyle.Italic, style.fontStyle)
    }

    @Test
    fun unknownLanguageAndPlainTextKeepTextWithoutStyles() {
        val code = "val a = 1"
        listOf(null, CodeLanguage.TEXT).forEach { language ->
            val text = highlightedCode(code, codeSpans(code, language), palette)
            assertEquals(code, text.text)
            assertTrue(text.spanStyles.isEmpty())
        }
    }
}
