package ru.aokazantsev.devatlas.themeformat.validation

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
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
    fun brokenFixtureReportsEveryKindOfMistake() {
        val result = validate("themeformat/src/jvmTest/resources/fixtures/broken-theme")

        val codes = result.issues.map { it.code }.toSet()
        val expected = setOf(
            IssueCode.ID_DUPLICATE, IssueCode.PAGE_FILE_MISSING, IssueCode.LINK_HTTP, IssueCode.LINK_PAGE_MISSING,
            IssueCode.PDF_DOWNLOAD_INVALID, IssueCode.TAG_UNKNOWN, IssueCode.IMAGE_MISSING, IssueCode.COLOR_LITERAL,
            IssueCode.COLOR_ROLE_UNKNOWN, IssueCode.SVG_FORBIDDEN, IssueCode.FILE_UNSUPPORTED,
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
