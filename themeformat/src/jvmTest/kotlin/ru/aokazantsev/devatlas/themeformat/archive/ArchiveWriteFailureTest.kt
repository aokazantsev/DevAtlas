package ru.aokazantsev.devatlas.themeformat.archive

import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Sink
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArchiveWriteFailureTest {

    private val workDir: File = Files.createTempDirectory("themeformat-write").toFile()
    private val target: Path = File(workDir, "out").toOkioPath()

    @AfterTest
    fun cleanUp() {
        workDir.deleteRecursively()
    }

    private fun archive(): Path {
        val file = File(workDir, "theme.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            listOf("manifest.json", "a.html").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write("{}".encodeToByteArray())
                zip.closeEntry()
            }
        }
        return file.toOkioPath()
    }

    @Test
    fun diskFullWhileUnpackingIsNotBlamedOnArchive() {
        val fullDisk = object : ForwardingFileSystem(FileSystem.SYSTEM) {
            override fun sink(file: Path, mustCreate: Boolean): Sink = throw IOException("No space left on device")
        }

        val result = ThemeArchiveExtractorImpl().extract(fullDisk, archive(), target)

        val issue = result.issues.single()
        assertEquals(IssueCode.ARCHIVE_UNREADABLE, issue.code)
        assertTrue(issue.message.startsWith("Не удалось записать распакованную тему"), issue.message)
        assertTrue("No space left" in issue.message)
        assertFalse(FileSystem.SYSTEM.exists(target))
    }

    @Test
    fun brokenArchiveIsStillReportedAsUnreadable() {
        val broken = File(workDir, "broken.zip").apply { writeText("not a zip") }.toOkioPath()

        val result = ThemeArchiveExtractorImpl().extract(FileSystem.SYSTEM, broken, target)

        assertTrue(result.issues.single().message.startsWith("Архив не читается"))
    }
}
