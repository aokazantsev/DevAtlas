package ru.aokazantsev.devatlas.themeformat.page

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlPageDuplicatesTest {

    private fun parse(html: String) = HtmlPageParser.parse("pages/p.html", html)

    @Test
    fun attributeWarningInsideFigureIsReportedOnce() {
        val page = parse(
            "<figure><img src=\"../d/x.svg\" alt=\"x\" width=\"10\"><figcaption>Подпись</figcaption><p data-x=\"1\">t</p></figure>",
        )

        val attributeWarnings = page.issues.filter { it.code == IssueCode.ATTRIBUTE_UNSUPPORTED }
        assertEquals(attributeWarnings.distinct(), attributeWarnings)
        assertEquals(1, attributeWarnings.size)
    }

    @Test
    fun attributeWarningInsideListItemIsReportedOnce() {
        val page = parse("<ul><li><b data-x=\"1\">a</b></li></ul>")

        assertEquals(1, page.issues.count { it.code == IssueCode.ATTRIBUTE_UNSUPPORTED })
    }

    @Test
    fun misplacedElementInListIsReportedOnce() {
        val page = parse("<ul><p>внутри списка</p></ul>")

        assertEquals(1, page.issues.count { it.code == IssueCode.TAG_MISPLACED })
    }

    @Test
    fun linkInsideLinkIsPlainTextAndItsTargetIsNotChecked() {
        val page = parse("<p><a href=\"page:outer\">до <a href=\"http://example.com\">внутри</a> после</a></p>")

        assertEquals(listOf("outer"), page.pageReferences.map { it.target })
        assertTrue(page.issues.none { it.code == IssueCode.LINK_HTTP })
        assertEquals(1, page.issues.count { it.code == IssueCode.TAG_MISPLACED })
        val paragraph = page.document.blocks.single() as PageBlock.Paragraph
        val link = paragraph.content.single() as PageInline.Link
        assertEquals("до внутри после", link.content.joinToString("") { (it as PageInline.Text).text })
    }

    @Test
    fun brokenInnerLinkInsideLinkDoesNotAddErrors() {
        val page = parse("<p><a href=\"page:a\"><a href=\"ftp://x\">t</a></a></p>")

        assertTrue(page.issues.none { it.code == IssueCode.LINK_UNSUPPORTED })
    }
}
