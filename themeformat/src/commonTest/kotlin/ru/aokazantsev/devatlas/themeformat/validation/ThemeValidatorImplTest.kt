package ru.aokazantsev.devatlas.themeformat.validation

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.themeformat.archive.ArchiveLimits
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeValidatorImplTest {

    private val fileSystem = FakeFileSystem()
    private val root = "/theme".toPath()

    private val manifest = """
        { "formatVersion": 1, "id": "demo", "title": "Демо", "tree": [
          { "id": "a", "title": "A", "file": "pages/a.html" },
          { "id": "s", "title": "S", "children": [ { "id": "b", "title": "B", "file": "pages/b.html" } ] }
        ] }
    """.trimIndent()

    private fun write(path: String, content: String) {
        val file = root / path
        file.parent?.let(fileSystem::createDirectories)
        fileSystem.write(file) { writeUtf8(content) }
    }

    private fun validate() = ThemeValidatorImpl().validate(fileSystem, root)

    private fun validTheme() {
        write("manifest.json", manifest)
        write("pages/a.html", "<p><a href=\"page:b\">b</a></p><img src=\"../d/x.svg\" alt=\"x\">")
        write("pages/b.html", "<p>b</p>")
        write("d/x.svg", "<svg viewBox=\"0 0 1 1\" fill=\"currentColor\"><rect fill=\"var(--primary)\"/></svg>")
    }

    @Test
    fun validThemePasses() {
        validTheme()
        write(".git/config", "ignored")
        write("README.md", "notes")

        val result = validate()

        assertTrue(result.passed, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        assertNotNull(result.manifest)
    }

    @Test
    fun missingManifestFails() {
        write("pages/a.html", "<p>a</p>")

        val result = validate()

        assertFalse(result.passed)
        assertEquals(listOf(IssueCode.MANIFEST_MISSING), result.issues.map { it.code })
    }

    @Test
    fun crossFileReferencesAreChecked() {
        validTheme()
        write("pages/a.html", "<p><a href=\"page:missing\">m</a> <a href=\"page:s\">s</a></p><img src=\"../d/none.svg\" alt=\"n\">")
        fileSystem.delete(root / "pages/b.html")

        val result = validate()

        assertEquals(
            listOf(
                IssueCode.FILE_UNUSED,
                IssueCode.PAGE_FILE_MISSING,
                IssueCode.LINK_PAGE_MISSING,
                IssueCode.LINK_PAGE_MISSING,
                IssueCode.IMAGE_MISSING,
            ),
            result.issues.map { it.code },
        )
        assertFalse(result.passed)
    }

    @Test
    fun folderRulesAreChecked() {
        validTheme()
        write("extra.png", "x")
        write("data.json", "{}")
        write("d/unused.svg", "<svg viewBox=\"0 0 1 1\" fill=\"currentColor\"/>")

        assertEquals(
            listOf(IssueCode.FILE_UNUSED, IssueCode.FILE_UNSUPPORTED, IssueCode.FILE_UNSUPPORTED),
            validate().issues.map { it.code },
        )
    }

    @Test
    fun namesThatArchiveRejectsAreErrorsInFolder() {
        validTheme()
        write("notes: draft.txt", "x")
        write("Pages/b.html", "<p>b</p>")

        val codes = validate().issues.map { it.code }
        assertEquals(2, codes.count { it == IssueCode.FILE_UNSUPPORTED })
    }

    @Test
    fun nonUtf8FileIsEncodingError() {
        validTheme()
        fileSystem.write(root / "pages/b.html") { write(byteArrayOf(0xEF.toByte(), 0xF0.toByte(), 0xE8.toByte())) }

        assertEquals(listOf(IssueCode.ENCODING), validate().issues.map { it.code })
    }

    @Test
    fun folderLimitsMatchArchiveLimits() {
        validTheme()
        val limits = ArchiveLimits.Default.copy(maxEntries = 3, maxDepth = 0, maxTotalBytes = 10)

        val result = ThemeValidatorImpl(limits).validate(fileSystem, root)
        val tooLarge = result.issues.filter { it.code == IssueCode.FILE_TOO_LARGE }

        assertTrue(IssueCode.TOO_MANY_FILES in result.issues.map { it.code })
        assertEquals(setOf("pages/a.html", "pages/b.html", "d/x.svg"), tooLarge.mapNotNull { it.file }.toSet())
        assertEquals(1, tooLarge.count { it.file == null })
        assertNull(result.manifest)
    }

    @Test
    fun everyFileIsCheckedAgainstFileLimit() {
        validTheme()
        write("NOTES.md", "x".repeat(2_000))
        write("pages/draft.html", "<p>${"x".repeat(2_000)}</p>")

        val result = ThemeValidatorImpl(ArchiveLimits.Default.copy(maxFileBytes = 1_000)).validate(fileSystem, root)

        assertEquals(
            setOf("NOTES.md", "pages/draft.html"),
            result.issues.filter { it.code == IssueCode.FILE_TOO_LARGE }.mapNotNull { it.file }.toSet(),
        )
    }

    @Test
    fun oversizedPageIsNotReadAndFileEqualToLimitPasses() {
        validTheme()
        write("pages/a.html", "<p>&infin;" + "x".repeat(2_000) + "</p>")
        write("pages/b.html", "<p>" + "x".repeat(993) + "</p>")

        val result = ThemeValidatorImpl(ArchiveLimits.Default.copy(maxFileBytes = 1_000)).validate(fileSystem, root)

        assertEquals(listOf(IssueCode.FILE_TOO_LARGE), result.issues.filter { it.file == "pages/a.html" }.map { it.code })
        assertTrue(result.issues.none { it.file == "pages/b.html" })
    }

    @Test
    fun fileSharedByPagesIsCheckedOnce() {
        validTheme()
        write("manifest.json", manifest.replace("pages/b.html", "pages/a.html"))
        write("pages/a.html", "<p>a</p><img src=\"../d/none.svg\" alt=\"n\">")

        val result = validate()

        assertEquals(1, result.issues.count { it.code == IssueCode.IMAGE_MISSING })
    }

    @Test
    fun issuesAreCappedPerFileAndPerTheme() {
        validTheme()
        write("pages/a.html", "<p>" + "<b a=1 style=x>".repeat(300) + "</p>")
        repeat(20) { write("d/bad$it.svg", "<svg>" + "<rect fill=\"#fff\"/>".repeat(200) + "</svg>") }

        val issues = validate().issues

        assertTrue(issues.groupBy { it.file }.values.all { it.size <= 101 })
        assertEquals(1, issues.count { it.code == IssueCode.TOO_MANY_ISSUES })
        assertTrue(issues.any { it.message.startsWith("И ещё замечаний в этом файле") })
    }

    @Test
    fun svgIssuesAreCollected() {
        validTheme()
        write("d/x.svg", "<svg viewBox=\"0 0 1 1\" fill=\"currentColor\"><rect fill=\"#fff\"/></svg>")

        val result = validate()

        assertEquals(listOf(IssueCode.COLOR_LITERAL), result.issues.map { it.code })
        assertEquals("d/x.svg", result.issues.single().file)
        assertTrue(result.issues.single().helpUrl.endsWith("#colors"))
    }
}
