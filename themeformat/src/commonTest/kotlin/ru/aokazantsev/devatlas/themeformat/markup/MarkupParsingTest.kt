package ru.aokazantsev.devatlas.themeformat.markup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkupParsingTest {

    @Test
    fun attributesInAllFormsAreRead() {
        val (nodes, problems) = MarkupDialect.Html.parse("<a href=\"x\" title='y' data=z download>t</a>")

        val link = nodes.single() as MarkupNode.Element
        assertEquals(listOf("href" to "x", "title" to "y", "data" to "z", "download" to null), link.attributes.map { it.name to it.value })
        assertTrue(problems.isEmpty())
    }

    @Test
    fun entitiesAreDecodedInTextAndAttributes() {
        val (nodes, _) = MarkupDialect.Html.parse("<a title=\"&laquo;x&raquo;\">a &lt; b &amp;&#x41;&#66; &unknown;</a>")

        val link = nodes.single() as MarkupNode.Element
        assertEquals("«x»", link.attribute("title")?.value)
        assertEquals("a < b &AB &unknown;", link.textContent())
    }

    @Test
    fun spacesBeforeEndTagBracketAreAccepted() {
        assertTrue(MarkupDialect.Html.parse("<p>x</p >").second.isEmpty())
        assertTrue(MarkupDialect.Xml.parse("<g></g\n>").second.isEmpty())
    }

    @Test
    fun manyAttributesAreReadQuickly() {
        val names = (0 until 50_000).map { "a$it" }
        val (nodes, _) = MarkupDialect.Html.parse("<p ${names.joinToString(" ")} a0>x</p>")

        assertEquals(names.size, (nodes.single() as MarkupNode.Element).attributes.size)
    }

    @Test
    fun numericEntitiesOutsideBmpAndOutOfRangeStayCorrect() {
        val (nodes, _) = MarkupDialect.Html.parse("<b>&#x1F600; &#0; &#x110000;</b>")

        assertEquals("😀 &#0; &#x110000;", (nodes.single() as MarkupNode.Element).textContent())
    }

    @Test
    fun commentsAndDoctypeAreSkippedAndPositionsAreCounted() {
        val (nodes, _) = MarkupDialect.Html.parse("<!doctype html>\n<!-- note -->\n  <p>x</p>")

        val paragraph = nodes.filterIsInstance<MarkupNode.Element>().single()
        assertEquals(SourcePosition(line = 3, column = 3), paragraph.position)
    }

    @Test
    fun scriptContentStaysRaw() {
        val (nodes, _) = MarkupDialect.Html.parse("<script>if (a < b) { x() }</script>")

        assertEquals("if (a < b) { x() }", (nodes.single() as MarkupNode.Element).textContent())
    }

    @Test
    fun unterminatedTagIsSyntaxProblem() {
        val (_, problems) = MarkupDialect.Html.parse("<p>text <b class=\"x\"")

        assertEquals(MarkupProblemKind.SYNTAX, problems.first().kind)
    }

    @Test
    fun optionalEndTagsAreImplied() {
        val (nodes, problems) = MarkupDialect.Html.parse("<ul><li>one<li>two</ul><p>a<p>b<table><tr><td>1<td>2<tr><td>3</table>")

        val elements = nodes.filterIsInstance<MarkupNode.Element>()
        assertEquals(listOf("ul", "p", "p", "table"), elements.map { it.name })
        assertEquals(2, elements[0].children.size)
        val rows = elements[3].children.filterIsInstance<MarkupNode.Element>()
        assertEquals(listOf(2, 1), rows.map { it.children.size })
        assertTrue(problems.isEmpty())
    }

    @Test
    fun strayAndUnclosedTagsAreStructureProblems() {
        val (_, problems) = MarkupDialect.Html.parse("<div><b>x</div></span>")

        assertEquals(listOf("Тег <b> не закрыт", "Лишний закрывающий тег </span>"), problems.map { it.message })
        assertTrue(problems.all { it.kind == MarkupProblemKind.STRUCTURE })
    }

    @Test
    fun xmlKeepsCaseAndHasNoVoidElements() {
        val (nodes, problems) = MarkupDialect.Xml.parse("<svg viewBox=\"0 0 1 1\"><foreignObject/><br></br></svg>")

        val svg = nodes.single() as MarkupNode.Element
        assertEquals("viewBox", svg.attributes.single().name)
        assertEquals(listOf("foreignObject", "br"), svg.children.map { (it as MarkupNode.Element).name })
        assertTrue(problems.isEmpty())
    }

    @Test
    fun nonLatinAfterLessThanIsText() {
        val (nodes, problems) = MarkupDialect.Html.parse("<p>adb install <путь к apk></p>")

        assertEquals("adb install <путь к apk>", (nodes.single() as MarkupNode.Element).textContent())
        assertTrue(problems.isEmpty())
    }

    @Test
    fun deepNestingIsCappedWithOneProblem() {
        val source = "<g>".repeat(5_000) + "</g>".repeat(5_000)

        val (_, problems) = MarkupDialect.Xml.parse(source)

        assertEquals(listOf(MarkupProblemKind.SYNTAX), problems.map { it.kind })
    }
}
