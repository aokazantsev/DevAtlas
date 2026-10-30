package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgCheckerTest {

    private fun svg(body: String, root: String = "viewBox=\"0 0 10 10\" fill=\"currentColor\"") =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" $root>$body</svg>"

    private fun codes(source: String) = SvgChecker.check("diagrams/a.svg", source).map { it.code }

    @Test
    fun tokensAndLocalReferencesPass() {
        val source = svg(
            "<defs><linearGradient id=\"g\"><stop stop-color=\"var(--primary)\"/></linearGradient></defs>" +
                "<rect fill=\"url(#g)\" stroke=\"var( --on-surface )\" style=\"fill: var(--tertiary-container); opacity: .5\"/>" +
                "<path fill=\"none\" stroke=\"currentColor\" marker-end=\"url(#g)\"/><use href=\"#g\"/>",
        )

        assertTrue(SvgChecker.check("a.svg", source).isEmpty())
    }

    @Test
    fun literalColorsAreErrors() {
        assertEquals(
            listOf(IssueCode.COLOR_LITERAL, IssueCode.COLOR_LITERAL, IssueCode.COLOR_LITERAL),
            codes(svg("<rect fill=\"#ff0000\"/><rect stroke=\"red\"/><rect style=\"stroke: rgb(1,2,3)\"/>")),
        )
    }

    @Test
    fun unknownRoleIsError() {
        assertEquals(listOf(IssueCode.COLOR_ROLE_UNKNOWN), codes(svg("<rect fill=\"var(--accent)\"/>")))
    }

    @Test
    fun unsafeContentIsForbidden() {
        assertEquals(
            listOf(IssueCode.SVG_FORBIDDEN, IssueCode.SVG_FORBIDDEN, IssueCode.SVG_EXTERNAL_REFERENCE, IssueCode.SVG_EXTERNAL_REFERENCE),
            codes(
                svg(
                    "<script>alert(1)</script><rect onclick=\"x()\"/><use xlink:href=\"http://example.com/a.svg#b\"/>" +
                        "<rect filter=\"url(https://example.com/f)\"/>",
                ),
            ),
        )
        assertEquals(listOf(IssueCode.SVG_FORBIDDEN), codes(svg("<style>rect { fill: red }</style>")))
        assertEquals(listOf(IssueCode.SVG_FORBIDDEN), codes(svg("<image href=\"a.png\"/>")))
    }

    @Test
    fun rootWithoutViewBoxOrFillIsWarned() {
        assertEquals(listOf(IssueCode.SVG_NO_VIEWBOX, IssueCode.SVG_NO_ROOT_FILL), codes(svg("", root = "")))
    }

    @Test
    fun brokenXmlAndWrongRootAreErrors() {
        assertTrue(IssueCode.SVG_SYNTAX in codes(svg("<rect>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes("<html></html>"))
    }

    @Test
    fun htmlLeniencyIsNotXml() {
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<rect width=10 fill=\"none\"/>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<rect hidden fill=\"none\"/>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<rect x=\"1\" x=\"2\" fill=\"none\"/>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<text>a&nbsp;b</text>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<text>A & B</text>")))
        assertTrue(codes(svg("<text>&lt;a&gt; &amp; &#160;&#xA0;</text>")).isEmpty())
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<text font-family=\"a&nbsp;b\">x</text>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<text aria-label=\"A & B\">x</text>")))
    }

    @Test
    fun lessThanAndCutEndTagAreXmlErrors() {
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<text>x < 5</text>")))
        assertEquals(listOf(IssueCode.SVG_SYNTAX), codes(svg("<text aria-label=\"a<b\">x</text>")))
        assertTrue(IssueCode.SVG_SYNTAX in codes(svg("<g></g <rect fill=\"none\"/>")))
    }

    @Test
    fun manyXmlProblemsStopWithoutCrash() {
        val source = svg("<text>&nbsp;</text>".repeat(120))

        val issues = SvgChecker.check("a.svg", source)

        assertTrue(issues.size in 2..101)
        assertTrue(issues.all { it.code == IssueCode.SVG_SYNTAX })
    }

    @Test
    fun editorExportPrologIsAccepted() {
        val source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- Generator: editor -->\n" +
            svg("<text><![CDATA[a < b & c]]></text><rect fill=\"none\"/>")

        assertTrue(codes(source).isEmpty())
    }

    @Test
    fun doctypeWithEntitiesIsError() {
        val source = "<!DOCTYPE svg [<!ENTITY x \"<image href='https://example.com/a.png'/>\">]>" + svg("<text>&x;</text>")

        assertTrue(IssueCode.SVG_SYNTAX in codes(source))
    }

    @Test
    fun longUrlValueWithSpacesIsCheckedQuickly() {
        val value = "url(#a" + " ".repeat(50_000) + "a"

        assertEquals(listOf(IssueCode.COLOR_LITERAL), codes(svg("<rect fill=\"$value\"/>")))
    }

    @Test
    fun everyColorRoleHasUniqueKebabName() {
        val names = ColorRole.entries.map { it.cssName }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { Regex("[a-z]+(-[a-z]+)*").matches(it) })
        assertEquals(ColorRole.ON_SURFACE_VARIANT, ColorRole.fromCssName("on-surface-variant"))
    }

    @Test
    fun externalUrlsInStyleAndMixedValuesAreErrors() {
        assertEquals(
            listOf(IssueCode.SVG_EXTERNAL_REFERENCE, IssueCode.SVG_EXTERNAL_REFERENCE, IssueCode.SVG_EXTERNAL_REFERENCE),
            codes(
                svg(
                    "<rect style=\"filter:url(https://example.com/f.svg#x)\"/>" +
                        "<rect filter=\"url(#a) url(https://example.com/b)\"/>" +
                        "<use xmlns:x=\"http://www.w3.org/1999/xlink\" x:href=\"https://example.com/a.svg\"/>",
                ),
            ),
        )
    }

    @Test
    fun renderBudgetOverflowIsError() {
        assertTrue(IssueCode.SVG_TOO_COMPLEX in codes(svg("""<g id="a"><use href="#b"/></g><g id="b"><use href="#a"/></g>""")))
        assertFalse(IssueCode.SVG_TOO_COMPLEX in codes(svg("""<defs><g id="a"><rect/></g></defs><use href="#a"/>""")))
        assertTrue(IssueCode.SVG_TOO_COMPLEX in codes(svg("<rect/>".repeat(20_001))))
        val broken = codes(svg("""<g id="a"><use href="#a"/></g><b>"""))
        assertTrue(IssueCode.SVG_SYNTAX in broken)
        assertFalse(IssueCode.SVG_TOO_COMPLEX in broken)
    }
}
