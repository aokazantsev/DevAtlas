package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.material3.lightColorScheme
import ru.aokazantsev.devatlas.designsystem.theme.AtlasColorRoles
import ru.aokazantsev.devatlas.themeformat.svg.ColorRole
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

    @Test
    fun textContentIsNotTouched() {
        val svg = """<text fill="var(--primary)">var(--primary) currentColor</text>"""

        assertEquals(
            """<text fill="#112233">var(--primary) currentColor</text>""",
            SvgColorTokens.resolve(svg, palette, "#000000"),
        )
    }

    @Test
    fun multilineStyleValueAndStyleElementAreResolved() {
        val svg = "<g style=\"fill: var(--primary);\n  stroke: var(--on-surface)\"/><style>.a { fill: var(--primary) }</style>"

        assertEquals(
            "<g style=\"fill: #112233;\n  stroke: #445566\"/><style>.a { fill: #112233 }</style>",
            SvgColorTokens.resolve(svg, palette, "#000000"),
        )
    }

    @Test
    fun designSystemRolesAreExactlyTheFormatRoles() {
        assertEquals(ColorRole.entries.map { it.cssName }.toSet(), AtlasColorRoles.colors(lightColorScheme()).keys)
    }

    @Test
    fun everyFormatRoleResolvesToItsOwnColor() {
        val colors = ColorRole.entries.mapIndexed { index, role -> role.cssName to hex(index + 1) }.toMap()

        ColorRole.entries.forEach { role ->
            val resolved = SvgColorTokens.resolve("""<rect fill="var(--${role.cssName})"/>""", colors, "#ffffff")

            assertEquals("""<rect fill="${colors.getValue(role.cssName)}"/>""", resolved, role.cssName)
        }
    }

    private fun hex(value: Int) = "#" + value.toString(HEX_RADIX).padStart(HEX_DIGITS, '0')

    private companion object {
        const val HEX_RADIX = 16
        const val HEX_DIGITS = 6
    }
}
