package ru.aokazantsev.devatlas.ui.page.diagram

import kotlin.test.Test
import kotlin.test.assertEquals

class SvgColorTokensTest {

    private val palette = mapOf("primary" to "#112233", "on-surface" to "#445566")

    @Test
    fun rolesAndCurrentColorBecomeHexColors() {
        val svg = """<svg fill="currentColor"><rect fill="var(--primary)" style="stroke: var( --on-surface );color:CurrentColor"/></svg>"""

        assertEquals(
            """<svg fill="#000000"><rect fill="#112233" style="stroke: #445566;color:#000000"/></svg>""",
            SvgColorTokens.resolve(svg, palette, textColor = "#000000"),
        )
    }

    @Test
    fun unknownRoleFallsBackToTextColor() {
        assertEquals("""<rect fill="#000000"/>""", SvgColorTokens.resolve("""<rect fill="var(--nope)"/>""", palette, "#000000"))
    }

    @Test
    fun otherValuesStayAsIs() {
        val svg = """<rect fill="url(#grad)" stroke="none"/>"""

        assertEquals(svg, SvgColorTokens.resolve(svg, palette, "#000000"))
    }
}
