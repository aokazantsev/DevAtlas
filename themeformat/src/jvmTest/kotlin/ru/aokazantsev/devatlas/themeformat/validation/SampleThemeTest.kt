package ru.aokazantsev.devatlas.themeformat.validation

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.aokazantsev.devatlas.themeformat.code.CodeHighlighter
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themeformat.page.HtmlPageParser
import ru.aokazantsev.devatlas.themeformat.page.model.InlineStyle
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleThemeTest {

    private val repository: Path = generateSequence(".".toPath().let(FileSystem.SYSTEM::canonicalize)) { it.parent }
        .first { FileSystem.SYSTEM.exists(it / "settings.gradle.kts") }

    private fun validate(folder: String) = ThemeValidatorImpl().validate(FileSystem.SYSTEM, repository / folder)

    @Test
    fun sampleThemeIsCleanAndUsesEveryFeature() {
        val result = validate("sample-theme")

        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { "${it.location} ${it.message}" })
        val manifest = checkNotNull(result.manifest)
        assertEquals("devatlas-sample", manifest.id)
        assertTrue(depth(manifest.tree) in 3..4)

        val sample = repository / "sample-theme"
        val blocks = manifest.pages.flatMap { page ->
            val source = FileSystem.SYSTEM.read(sample / page.file) { readUtf8() }
            flatten(HtmlPageParser.parse(page.file, source).document.blocks)
        }
        val blockTypes = blocks.map { it::class }.toSet()
        val expectedTypes = setOf(
            PageBlock.Heading::class, PageBlock.Paragraph::class, PageBlock.ListBlock::class, PageBlock.Code::class,
            PageBlock.Quote::class, PageBlock.Table::class, PageBlock.Figure::class, PageBlock.Divider::class,
        )
        assertEquals(expectedTypes, blockTypes)
        assertEquals((1..6).toSet(), blocks.filterIsInstance<PageBlock.Heading>().map { it.level }.toSet())
        val languages = blocks.filterIsInstance<PageBlock.Code>().mapNotNull { block -> block.language?.let(CodeLanguage::resolve) }
        assertEquals(CodeLanguage.entries.toSet(), languages.toSet())

        val inlines = blocks.flatMap(::inlinesOf).flatMap(::flattenInline)
        val links = inlines.filterIsInstance<PageInline.Link>()
        assertEquals(
            setOf(LinkTarget.Page::class, LinkTarget.Web::class, LinkTarget.Email::class, LinkTarget.Phone::class),
            links.map { it.target::class }.toSet(),
        )
        assertTrue(links.any { it.download?.endsWith(".pdf") == true })
        val styles = inlines.filterIsInstance<PageInline.Text>().map { it.style }
        assertTrue(styles.any { it.bold } && styles.any { it.italic } && styles.any { it.code })
        assertTrue(inlines.any { it == PageInline.LineBreak })
        assertTrue(styles.contains(InlineStyle.Plain))
    }

    @Test
    fun sampleCodeBlocksHighlightWithoutArtifacts() {
        val source = FileSystem.SYSTEM.read(repository / "sample-theme/pages/text/code.html") { readUtf8() }
        val byLanguage = flatten(HtmlPageParser.parse("pages/text/code.html", source).document.blocks)
            .filterIsInstance<PageBlock.Code>()
            .associateBy { it.language }

        fun tokens(language: String): List<Pair<String, CodeTokenKind>> {
            val block = checkNotNull(byLanguage[language]) { language }
            return CodeHighlighter.highlight(block.code, checkNotNull(CodeLanguage.resolve(language)))
                .map { block.code.substring(it.start, it.end) to it.kind }
        }

        val csharp = tokens("csharp")
        assertTrue("@\"C:\\themes\"" to CodeTokenKind.STRING in csharp, "$csharp")
        assertTrue(csharp.any { it.second == CodeTokenKind.STRING && it.first.startsWith("$\"") }, "$csharp")
        assertTrue(csharp.any { it.second == CodeTokenKind.COMMENT && it.first.startsWith("//") }, "$csharp")
        assertTrue("record" to CodeTokenKind.KEYWORD in csharp, "$csharp")

        val xml = tokens("xml")
        assertTrue(xml.first().second == CodeTokenKind.META && xml.first().first.startsWith("<?xml"), "$xml")
        assertTrue(xml.any { it.second == CodeTokenKind.COMMENT }, "$xml")
        val tags = xml.filter { it.first.startsWith("<") && it.second == CodeTokenKind.TAG }.map { it.first }
        assertEquals(listOf("<LinearLayout", "<TextView", "</LinearLayout"), tags)
        val attributes = xml.filter { it.second == CodeTokenKind.ATTRIBUTE }.map { it.first }
        assertTrue(attributes.all { it.startsWith("android:") || it.startsWith("xmlns:") }, "$attributes")

        val diff = tokens("diff")
        val headers = diff.filter { it.second == CodeTokenKind.META && (it.first.startsWith("---") || it.first.startsWith("+++")) }
        assertEquals(listOf("--- a/manifest.json", "+++ b/manifest.json"), headers.map { it.first })
        assertEquals(1, diff.count { it.second == CodeTokenKind.DELETED }, "$diff")
        assertEquals(1, diff.count { it.second == CodeTokenKind.INSERTED }, "$diff")

        val sql = tokens("sql")
        assertEquals(CodeTokenKind.COMMENT, sql.first().second, "$sql")
        val clauses = listOf("SELECT", "FROM", "LEFT", "JOIN", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT")
        assertTrue(clauses.all { it to CodeTokenKind.KEYWORD in sql }, "$sql")
        assertTrue("'devatlas-sample'" to CodeTokenKind.STRING in sql, "$sql")
        assertTrue("'2026-10-01'" to CodeTokenKind.STRING in sql, "$sql")
        assertEquals(listOf("0", "10"), sql.filter { it.second == CodeTokenKind.NUMBER }.map { it.first })
    }

    @Test
    fun brokenFixtureReportsEveryKindOfMistake() {
        val result = validate("themeformat/src/jvmTest/resources/fixtures/broken-theme")

        val codes = result.issues.map { it.code }.toSet()
        val expected = setOf(
            IssueCode.ID_DUPLICATE, IssueCode.PAGE_FILE_MISSING, IssueCode.LINK_HTTP, IssueCode.LINK_PAGE_MISSING,
            IssueCode.PDF_DOWNLOAD_INVALID, IssueCode.TAG_UNKNOWN, IssueCode.IMAGE_MISSING, IssueCode.COLOR_LITERAL,
            IssueCode.COLOR_ROLE_UNKNOWN, IssueCode.SVG_FORBIDDEN, IssueCode.FILE_UNSUPPORTED, IssueCode.CODE_LANGUAGE,
            IssueCode.CODE_JSON_INVALID,
        )
        assertTrue(codes.containsAll(expected), "нет: ${expected - codes}")
        assertTrue(result.issues.all { it.helpUrl.startsWith("https://aokazantsev.ru/pets/devatlas/themes/#") })
    }

    private fun depth(nodes: List<ThemeNode>): Int =
        1 + (nodes.filterIsInstance<ThemeNode.Section>().maxOfOrNull { depth(it.children) } ?: 0)

    private fun flatten(blocks: List<PageBlock>): List<PageBlock> = blocks.flatMap { block ->
        listOf(block) + when (block) {
            is PageBlock.ListBlock -> block.items.flatMap(::flatten)
            is PageBlock.Quote -> flatten(block.blocks)
            else -> emptyList()
        }
    }

    private fun inlinesOf(block: PageBlock): List<PageInline> = when (block) {
        is PageBlock.Heading -> block.content
        is PageBlock.Paragraph -> block.content
        is PageBlock.Table -> block.rows.flatMap { row -> row.cells.flatMap { it.content } }
        is PageBlock.Figure -> block.caption
        else -> emptyList()
    }

    private fun flattenInline(inline: PageInline): List<PageInline> =
        listOf(inline) + if (inline is PageInline.Link) inline.content.flatMap(::flattenInline) else emptyList()
}
