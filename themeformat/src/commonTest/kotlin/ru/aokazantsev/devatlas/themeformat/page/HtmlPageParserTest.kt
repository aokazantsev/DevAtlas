package ru.aokazantsev.devatlas.themeformat.page

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.page.model.InlineStyle
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.themeformat.page.model.TableCell
import ru.aokazantsev.devatlas.themeformat.page.model.TableRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlPageParserTest {

    private fun parse(html: String, file: String = "pages/page.html") = HtmlPageParser.parse(file, html)

    private fun codes(html: String) = parse(html).issues.map { it.code }

    @Test
    fun headingsAndParagraphsWithInlineStyles() {
        val page = parse("<h2>Title</h2>\n<p>  a <b>bold <i>both</i></b>\n <code>x()</code><br> next  </p>")

        assertEquals(
            listOf(
                PageBlock.Heading(2, listOf(PageInline.Text("Title"))),
                PageBlock.Paragraph(
                    listOf(
                        PageInline.Text("a "),
                        PageInline.Text("bold ", InlineStyle(bold = true)),
                        PageInline.Text("both", InlineStyle(bold = true, italic = true)),
                        PageInline.Text(" "),
                        PageInline.Text("x()", InlineStyle(code = true)),
                        PageInline.LineBreak,
                        PageInline.Text("next"),
                    ),
                ),
            ),
            page.document.blocks,
        )
        assertTrue(page.issues.isEmpty())
    }

    @Test
    fun looseTextBecomesParagraph() {
        assertEquals(listOf(PageBlock.Paragraph(listOf(PageInline.Text("loose text")))), parse("loose\ntext").document.blocks)
    }

    @Test
    fun nestedListsKeepStructure() {
        val blocks = parse("<ol><li>one<ul><li>inner</li></ul></li><li>two</li></ol>").document.blocks

        val list = blocks.single() as PageBlock.ListBlock
        assertTrue(list.ordered)
        assertEquals(2, list.items.size)
        assertEquals(PageBlock.ListBlock(false, listOf(listOf(PageBlock.Paragraph(listOf(PageInline.Text("inner")))))), list.items[0][1])
    }

    @Test
    fun codeBlockKeepsWhitespaceAndLanguage() {
        val blocks = parse("<pre><code class=\"language-kotlin\">\nfun a() {\n    b &lt; c\n}\n</code></pre>").document.blocks

        assertEquals(listOf(PageBlock.Code("fun a() {\n    b < c\n}", "kotlin")), blocks)
    }

    @Test
    fun tableReadsHeaderAndBodyCells() {
        val table = parse("<table><thead><tr><th>A</th></tr></thead><tbody><tr><td>1</td></tr></tbody></table>").document.blocks.single()

        assertEquals(
            PageBlock.Table(
                listOf(
                    TableRow(listOf(TableCell(true, listOf(PageInline.Text("A"))))),
                    TableRow(listOf(TableCell(false, listOf(PageInline.Text("1"))))),
                ),
            ),
            table,
        )
    }

    @Test
    fun figureResolvesImageAndCaption() {
        val page = parse("<figure><img src=\"../diagrams/a.svg\" alt=\"Схема\"><figcaption>Подпись</figcaption></figure>")

        assertEquals(listOf(PageBlock.Figure("diagrams/a.svg", "Схема", listOf(PageInline.Text("Подпись")))), page.document.blocks)
        assertEquals(listOf("diagrams/a.svg"), page.imageReferences.map { it.target })
    }

    @Test
    fun imageInsideParagraphSplitsIt() {
        val blocks = parse("<p>before <img src=\"a.svg\" alt=\"x\"> after</p>").document.blocks

        assertEquals(3, blocks.size)
        assertEquals(PageBlock.Figure("pages/a.svg", "x", emptyList()), blocks[1])
    }

    @Test
    fun imageRulesAreChecked() {
        assertEquals(listOf(IssueCode.IMAGE_SOURCE), codes("<img src=\"https://example.com/a.svg\" alt=\"x\">"))
        assertEquals(listOf(IssueCode.IMAGE_SOURCE), codes("<img src=\"a.png\" alt=\"x\">"))
        assertEquals(listOf(IssueCode.IMAGE_SOURCE), codes("<img src=\"../../a.svg\" alt=\"x\">"))
        assertEquals(listOf(IssueCode.IMAGE_ALT_MISSING), codes("<img src=\"a.svg\">"))
        assertEquals(listOf(IssueCode.TAG_MISPLACED), codes("<p><b><img src=\"a.svg\" alt=\"x\"></b></p>"))
    }

    @Test
    fun linkTargetsAreParsed() {
        val page = parse(
            "<p><a href=\"page:intro\">i</a><a href=\"https://example.com/x\">w</a>" +
                "<a href=\"mailto:a@example.com\">m</a><a href=\"tel:+700\">t</a></p>",
        )

        val links = (page.document.blocks.single() as PageBlock.Paragraph).content.filterIsInstance<PageInline.Link>()
        assertEquals(
            listOf(
                LinkTarget.Page("intro"),
                LinkTarget.Web("https://example.com/x"),
                LinkTarget.Email("mailto:a@example.com"),
                LinkTarget.Phone("tel:+700"),
            ),
            links.map { it.target },
        )
        assertEquals(listOf("intro"), page.pageReferences.map { it.target })
        assertTrue(page.issues.isEmpty())
    }

    @Test
    fun badLinksBecomeTextWithErrors() {
        val page = parse("<p><a href=\"http://example.com\">h</a> <a href=\"other.html\">r</a> <a href=\"page:Bad_Id\">p</a> <a>e</a></p>")

        assertEquals(
            listOf(IssueCode.LINK_HTTP, IssueCode.LINK_UNSUPPORTED, IssueCode.LINK_PAGE_MISSING, IssueCode.LINK_EMPTY),
            page.issues.map { it.code },
        )
        assertEquals(listOf(PageBlock.Paragraph(listOf(PageInline.Text("h r p e")))), page.document.blocks)
    }

    @Test
    fun pdfLinksNeedValidDownloadName() {
        val ok = parse("<a href=\"https://example.com/doc.pdf?v=1\" download=\"doc.pdf\">pdf</a>")
        val link = (ok.document.blocks.single() as PageBlock.Paragraph).content.single() as PageInline.Link
        assertEquals("doc.pdf", link.download)
        assertTrue(ok.issues.isEmpty())

        assertEquals(listOf(IssueCode.PDF_DOWNLOAD_MISSING), codes("<a href=\"https://example.com/doc.pdf\">pdf</a>"))
        assertEquals(listOf(IssueCode.PDF_DOWNLOAD_INVALID), codes("<a href=\"https://example.com/doc.pdf\" download=\"a/b.pdf\">x</a>"))
        assertEquals(listOf(IssueCode.PDF_DOWNLOAD_INVALID), codes("<a href=\"https://example.com/doc.pdf\" download=\"doc.txt\">x</a>"))
        assertEquals(listOf(IssueCode.DOWNLOAD_NOT_PDF), codes("<a href=\"https://example.com/\" download=\"a.pdf\">x</a>"))
    }

    @Test
    fun missingDownloadWarningNamesTheWholeFileNameChain() {
        val message = parse("<a href=\"https://example.com/doc.pdf\">pdf</a>").issues.single().message

        listOf("ответа сервера", "адреса", "заголовка страницы", "домена с датой").forEach { part ->
            assertTrue(part in message, "нет «$part» в «$message»")
        }
    }

    @Test
    fun unknownTagsBecomeTextWithWarning() {
        val page = parse("<div><span>hello</span></div><script>alert(1)</script><p style=\"color:red\" onclick=\"x\">p</p>")

        assertEquals(
            listOf(PageBlock.Paragraph(listOf(PageInline.Text("hello"))), PageBlock.Paragraph(listOf(PageInline.Text("p")))),
            page.document.blocks,
        )
        assertEquals(
            listOf(
                IssueCode.TAG_UNKNOWN,
                IssueCode.TAG_UNKNOWN,
                IssueCode.TAG_UNKNOWN,
                IssueCode.ATTRIBUTE_UNSUPPORTED,
                IssueCode.ATTRIBUTE_UNSUPPORTED,
            ),
            page.issues.map { it.code },
        )
        assertTrue(page.issues.all { it.severity == IssueSeverity.WARNING })
    }

    @Test
    fun documentWrapperIsError() {
        val page = parse("<!DOCTYPE html><html><head><title>t</title><body><p>x</p></body></html>")

        assertEquals(listOf(IssueCode.PAGE_WRAPPER, IssueCode.PAGE_WRAPPER), page.issues.map { it.code })
        assertTrue(page.issues.all { it.severity == IssueSeverity.ERROR })
    }

    @Test
    fun spacesAtLinkEdgesAreKept() {
        val content = (parse("<p>см.<a href=\"page:x\"> тут </a>(PDF)</p>").document.blocks.single() as PageBlock.Paragraph).content

        assertEquals(
            listOf(
                PageInline.Text("см. "),
                PageInline.Link(LinkTarget.Page("x"), download = null, content = listOf(PageInline.Text("тут"))),
                PageInline.Text(" (PDF)"),
            ),
            content,
        )
    }

    @Test
    fun problemsFromTextBeforeTagsStopWithoutCrash() {
        val issues = parse("<p>&zz;</p>".repeat(120)).issues

        assertTrue(issues.any { it.message.startsWith("Слишком много ошибок разметки") })
    }

    @Test
    fun tagsInsideInlineCodeAreErrors() {
        assertTrue(IssueCode.CODE_MARKUP in codes("<p>Тип <code>List<String></code></p>"))
        assertTrue(codes("<p><code>a <b>b</b> <i>c</i></code></p>").isEmpty())
    }

    @Test
    fun unclosedStyleIsErrorAndRestIsKept() {
        val page = parse("<p><code><style></code> текст</p>")

        assertTrue(IssueCode.MARKUP_SYNTAX in page.issues.map { it.code })
        assertTrue(page.document.blocks.toString().contains("текст"))
    }

    @Test
    fun markupProblemsStopAfterLimit() {
        val issues = parse("<p>" + "<a".repeat(1_000)).issues

        assertTrue(issues.size <= 101)
        assertTrue(issues.any { it.message.startsWith("Слишком много ошибок разметки") })
    }

    @Test
    fun syntaxErrorsCarryPosition() {
        val issue = parse("<p>ok</p>\n<p>text <b").issues.single()

        assertEquals(IssueCode.MARKUP_SYNTAX, issue.code)
        assertEquals(2 to 9, issue.line to issue.column)
        assertEquals("pages/page.html:2:9", issue.location)
    }

    @Test
    fun tagCutOffByNextTagIsError() {
        val page = parse("<p>Текст <b жирный</b> дальше</p>")

        assertTrue(IssueCode.MARKUP_SYNTAX in page.issues.map { it.code })
        assertTrue(page.document.blocks.toString().contains("дальше"))
    }

    @Test
    fun endTagCutOffIsErrorAndTextIsKept() {
        val page = parse("<p><b>жирный</b дальше <i>курсив</i></p>")

        assertTrue(IssueCode.MARKUP_SYNTAX in page.issues.map { it.code })
        assertTrue(page.document.blocks.toString().contains("дальше"))
    }

    @Test
    fun tagsInsideCodeBlockAreErrors() {
        assertEquals(
            listOf(IssueCode.CODE_MARKUP),
            codes("<pre><code class=\"language-xml\"><TextView android:id=\"@+id/x\" /></code></pre>"),
        )
        assertEquals(listOf(IssueCode.CODE_MARKUP), codes("<pre><code class=\"language-text\">a<br>b</code></pre>"))
        assertTrue(codes("<pre><code class=\"language-kotlin\">List&lt;String&gt; &amp;</code></pre>").isEmpty())
    }

    @Test
    fun unknownNamedEntityIsWarned() {
        val page = parse("<p>ab &infin; &laquo;x&raquo;</p>")
        val issue = page.issues.single()

        assertEquals(IssueCode.ENTITY_UNKNOWN, issue.code)
        assertEquals(IssueSeverity.WARNING, issue.severity)
        assertEquals(1 to 7, issue.line to issue.column)
    }

    @Test
    fun textAroundLinkKeepsOrderAndSpaces() {
        val content = (parse("<p>см. <a href=\"page:x\">тут</a>, дальше</p>").document.blocks.single() as PageBlock.Paragraph).content

        assertEquals(
            listOf(
                PageInline.Text("см. "),
                PageInline.Link(LinkTarget.Page("x"), download = null, content = listOf(PageInline.Text("тут"))),
                PageInline.Text(", дальше"),
            ),
            content,
        )
    }

    @Test
    fun manyTextFragmentsAndAmpersandsStayLinear() {
        val fragments = parse("<p>" + "x<!---->".repeat(20_000) + "</p>")
        val ampersands = parse("<p>" + "&a".repeat(50_000) + "</p>")

        assertEquals(listOf(PageBlock.Paragraph(listOf(PageInline.Text("x".repeat(20_000))))), fragments.document.blocks)
        assertEquals(listOf(PageBlock.Paragraph(listOf(PageInline.Text("&a".repeat(50_000))))), ampersands.document.blocks)
    }

    @Test
    fun extraFigureContentIsKeptAfterImage() {
        val page = parse("<figure><img src=\"a.svg\" alt=\"A\"><img src=\"b.svg\" alt=\"B\"><p>Пояснение</p></figure>")

        assertEquals(3, page.document.blocks.size)
        assertEquals(listOf("pages/a.svg", "pages/b.svg"), page.imageReferences.map { it.target })
        assertEquals(listOf(IssueCode.TAG_MISPLACED), page.issues.map { it.code })
    }
}
