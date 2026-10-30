package ru.aokazantsev.devatlas.themeformat.archive

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchiveEntryCountTest {

    private val fileSystem = FileSystem.SYSTEM
    private val workDir: File = Files.createTempDirectory("themeformat-count").toFile()
    private val target: Path = File(workDir, "out").toOkioPath()

    @AfterTest
    fun cleanUp() {
        workDir.deleteRecursively()
    }

    private fun zip(names: List<String>): Path {
        val file = File(workDir, "theme-${names.size}.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            names.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                if (!name.endsWith("/")) zip.write("{}".encodeToByteArray())
                zip.closeEntry()
            }
        }
        return file.toOkioPath()
    }

    private fun extract(names: List<String>, limits: ArchiveLimits) =
        ThemeArchiveExtractorImpl(limits).extract(fileSystem, zip(names), target)

    @Test
    fun directoriesAndMacResourcesDoNotCountAgainstFileLimit() {
        val folders = (1..10).map { "folder$it/" }
        val resources = (1..10).map { "__MACOSX/._file$it" }
        val files = listOf("manifest.json", "a.html")

        val result = extract(files + folders + resources, ArchiveLimits.Default.copy(maxEntries = 2))

        assertTrue(result.succeeded, result.issues.toString())
        assertEquals(files.sorted(), result.files.sorted())
    }

    @Test
    fun realFilesAboveLimitAreRejected() {
        val result = extract(listOf("manifest.json", "a.html", "b.html"), ArchiveLimits.Default.copy(maxEntries = 2))

        assertEquals(listOf(IssueCode.ARCHIVE_LIMIT), result.issues.map { it.code })
        assertTrue("файлов" in result.issues.single().message)
    }

    @Test
    fun rawDirectoryAboveSeparateLimitIsRejectedBeforeAnythingElse() {
        val names = listOf("manifest.json") + (1..5).map { "folder$it/" }

        val result = extract(names, ArchiveLimits.Default.copy(maxRawEntries = 5))

        assertEquals(listOf(IssueCode.ARCHIVE_LIMIT), result.issues.map { it.code })
        assertTrue("каталоге архива" in result.issues.single().message)
    }

    @Test
    fun filesOutsideRequestedSubFolderDoNotCountOrFail() {
        val theme = listOf("repo-main/themes/a/manifest.json", "repo-main/themes/a/a.html")
        val other = (1..5).map { "repo-main/src/file$it.kt" } + "repo-main/docs/../escape.md"

        val result = ThemeArchiveExtractorImpl(ArchiveLimits.Default.copy(maxEntries = 2))
            .extract(fileSystem, zip(theme + other), target, subFolder = "themes/a")

        assertTrue(result.succeeded, result.issues.toString())
        assertEquals(listOf("a.html", "manifest.json"), result.files.sorted())
    }

    @Test
    fun segmentWithTrailingSpaceInEntryNameIsRejected() {
        val result = extract(listOf("manifest.json", "pages /a.html"), ArchiveLimits.Default)

        assertEquals(listOf(IssueCode.ARCHIVE_UNSAFE_PATH), result.issues.map { it.code })
    }
}
