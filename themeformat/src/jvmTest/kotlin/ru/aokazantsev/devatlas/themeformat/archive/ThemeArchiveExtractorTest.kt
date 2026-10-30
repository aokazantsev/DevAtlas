package ru.aokazantsev.devatlas.themeformat.archive

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeArchiveExtractorTest {

    private val fileSystem = FileSystem.SYSTEM
    private val workDir: File = Files.createTempDirectory("themeformat-archive").toFile()
    private val target: Path = File(workDir, "out").toOkioPath()

    @AfterTest
    fun cleanUp() {
        workDir.deleteRecursively()
    }

    private fun zip(vararg entries: Pair<String, ByteArray>, stored: Boolean = false): Path {
        val file = File(workDir, "theme-${entries.size}-$stored.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) -> zip.add(name, bytes, stored) }
        }
        return file.toOkioPath()
    }

    private fun ZipOutputStream.add(name: String, bytes: ByteArray, stored: Boolean) {
        val entry = ZipEntry(name)
        if (stored) {
            entry.method = ZipEntry.STORED
            entry.size = bytes.size.toLong()
            entry.crc = CRC32().apply { update(bytes) }.value
        }
        putNextEntry(entry)
        write(bytes)
        closeEntry()
    }

    private fun text(value: String) = value.encodeToByteArray()

    private fun extract(archive: Path, limits: ArchiveLimits = ArchiveLimits.Default) =
        ThemeArchiveExtractorImpl(limits).extract(fileSystem, archive, target)

    @Test
    fun rootLevelThemeIsExtracted() {
        val result = extract(zip("manifest.json" to text("{}"), "pages/a.html" to text("<p>a</p>"), "pages/" to ByteArray(0)))

        assertTrue(result.succeeded, result.issues.toString())
        assertEquals(listOf("manifest.json", "pages/a.html"), result.files)
        assertEquals("<p>a</p>", fileSystem.read(target / "pages/a.html") { readUtf8() })
    }

    @Test
    fun singleNestedFolderBecomesRoot() {
        val result = extract(
            zip(
                "theme-main/manifest.json" to text("{}"),
                "theme-main/a.html" to text("a"),
                "__MACOSX/theme-main/._a.html" to text("x"),
                "theme-main/.DS_Store" to text("x"),
            ),
        )

        assertTrue(result.succeeded, result.issues.toString())
        assertEquals(listOf("manifest.json", "a.html"), result.files)
    }

    @Test
    fun storedEntriesAreExtracted() {
        val result = extract(zip("manifest.json" to text("{\"a\":1}"), stored = true))

        assertTrue(result.succeeded, result.issues.toString())
        assertEquals("{\"a\":1}", fileSystem.read(target / "manifest.json") { readUtf8() })
    }

    @Test
    fun pathTraversalRejectsWholeArchiveAndCleansTarget() {
        val result = extract(zip("manifest.json" to text("{}"), "../evil.txt" to text("x")))

        assertEquals(listOf(IssueCode.ARCHIVE_UNSAFE_PATH), result.issues.map { it.code })
        assertFalse(fileSystem.exists(target))
        assertFalse(File(workDir, "evil.txt").exists())
    }

    @Test
    fun absolutePathAndDuplicatesAreRejected() {
        val absolute = extract(zip("manifest.json" to text("{}"), "/etc/x" to text("x")))
        assertEquals(listOf(IssueCode.ARCHIVE_UNSAFE_PATH), absolute.issues.map { it.code })
        assertEquals(
            listOf(IssueCode.ARCHIVE_UNSAFE_PATH),
            extract(zip("manifest.json" to text("{}"), "A.html" to text("x"), "a.html" to text("y"))).issues.map { it.code },
        )
    }

    @Test
    fun missingManifestIsReported() {
        val result = extract(zip("a/manifest.json" to text("{}"), "b/x.html" to text("x")))

        assertEquals(listOf(IssueCode.ARCHIVE_NO_ROOT), result.issues.map { it.code })
    }

    @Test
    fun limitsAreEnforced() {
        val many = (1..5).map { "p$it.html" to text("x") }.toTypedArray()
        assertEquals(
            listOf(IssueCode.ARCHIVE_LIMIT),
            extract(zip("manifest.json" to text("{}"), *many), ArchiveLimits.Default.copy(maxEntries = 3)).issues.map { it.code },
        )
        assertEquals(
            listOf(IssueCode.ARCHIVE_LIMIT),
            extract(
                zip("manifest.json" to text("{}"), "a/b/c/d.html" to text("x")),
                ArchiveLimits.Default.copy(maxDepth = 2),
            ).issues.map { it.code },
        )
        assertEquals(
            listOf(IssueCode.ARCHIVE_LIMIT),
            extract(zip("manifest.json" to ByteArray(4096)), ArchiveLimits.Default.copy(maxFileBytes = 1024)).issues.map { it.code },
        )
    }

    @Test
    fun fileBiggerThanDefaultLimitIsRejected() {
        val zeros = ByteArray(3 * ArchiveLimits.MEGABYTE.toInt())

        assertEquals(listOf(IssueCode.ARCHIVE_LIMIT), extract(zip("manifest.json" to text("{}"), "a.html" to zeros)).issues.map { it.code })
    }

    @Test
    fun notZipIsUnreadable() {
        val file = File(workDir, "fake.zip").apply { writeText("not a zip at all, just text that is long enough") }

        assertEquals(listOf(IssueCode.ARCHIVE_UNREADABLE), extract(file.toOkioPath()).issues.map { it.code })
    }

    @Test
    fun absolutePathBehindRootFolderIsRejected() {
        val result = extract(zip("theme/manifest.json" to text("{}"), "theme//tmp/evil.txt" to text("x")))

        assertEquals(listOf(IssueCode.ARCHIVE_UNSAFE_PATH), result.issues.map { it.code })
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun dotSegmentsAreDroppedAndForbiddenCharactersRejected() {
        val dot = extract(zip("./" to ByteArray(0), "./manifest.json" to text("{}"), "./pages/./a.html" to text("x")))
        assertTrue(dot.succeeded, dot.issues.toString())
        assertEquals(listOf("manifest.json", "pages/a.html"), dot.files)
        fileSystem.deleteRecursively(target)

        val emptySegment = extract(zip("manifest.json" to text("{}"), "pages//a.html" to text("x")))
        assertEquals(listOf(IssueCode.ARCHIVE_UNSAFE_PATH), emptySegment.issues.map { it.code })

        val forbidden = extract(zip("manifest.json" to text("{}"), "a?/b.svg" to text("x")))
        assertEquals(listOf(IssueCode.ARCHIVE_UNSAFE_PATH), forbidden.issues.map { it.code })
    }

    @Test
    fun existingTargetIsNotTouched() {
        fileSystem.createDirectories(target)
        fileSystem.write(target / "keep.txt") { writeUtf8("old") }

        val result = extract(zip("manifest.json" to text("{}")))

        assertEquals(listOf(IssueCode.ARCHIVE_UNREADABLE), result.issues.map { it.code })
        assertEquals("old", fileSystem.read(target / "keep.txt") { readUtf8() })
    }

    @Test
    fun depthEqualToLimitIsAccepted() {
        val result = extract(
            zip("manifest.json" to text("{}"), "a/b/x.html" to text("x")),
            ArchiveLimits.Default.copy(maxDepth = 2),
        )

        assertTrue(result.succeeded, result.issues.toString())
    }

    @Test
    fun entriesSharingDataAreRejected() {
        val data = ByteArray(8000) { (it * 31 % 251).toByte() }
        val archive = zip("manifest.json" to text("{}"), "a.html" to data, "b.html" to text("x"), stored = true)
        patchCentral(archive, entryIndex = 2, field = CENTRAL_COMPRESSED_SIZE, value = data.size)
        patchCentral(archive, entryIndex = 2, field = CENTRAL_UNCOMPRESSED_SIZE, value = data.size)

        assertEquals(listOf(IssueCode.ARCHIVE_UNREADABLE), extract(archive).issues.map { it.code })
    }

    @Test
    fun fileEqualToLimitIsAccepted() {
        val result = extract(
            zip("manifest.json" to text("{}"), "a.html" to ByteArray(1024) { 'a'.code.toByte() }),
            ArchiveLimits.Default.copy(maxFileBytes = 1024),
        )

        assertTrue(result.succeeded, result.issues.toString())
    }

    @Test
    fun entryBiggerThanDeclaredIsRejectedAndTargetRemoved() {
        val archive = zip("manifest.json" to text("{}"), "a.html" to ByteArray(4096) { 'a'.code.toByte() })
        patchUncompressedSize(archive, entryIndex = 1, size = 16)

        val result = extract(archive)

        assertEquals(listOf(IssueCode.ARCHIVE_UNREADABLE), result.issues.map { it.code })
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun entrySmallerThanDeclaredIsRejectedAndTargetRemoved() {
        val archive = zip("manifest.json" to text("{}"), "a.html" to text("short"))
        patchUncompressedSize(archive, entryIndex = 1, size = 500)

        val result = extract(archive)

        assertEquals(listOf(IssueCode.ARCHIVE_UNREADABLE), result.issues.map { it.code })
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun totalSizeLimitIsEnforced() {
        val result = extract(
            zip("manifest.json" to ByteArray(2000), "a.html" to ByteArray(2000)),
            ArchiveLimits.Default.copy(maxTotalBytes = 3000),
        )

        assertEquals(listOf(IssueCode.ARCHIVE_LIMIT), result.issues.map { it.code })
        assertFalse(fileSystem.exists(target))
    }

    private fun patchUncompressedSize(archive: Path, entryIndex: Int, size: Int) {
        patchCentral(archive, entryIndex, CENTRAL_UNCOMPRESSED_SIZE, size)
    }

    private fun patchCentral(archive: Path, entryIndex: Int, field: Int, value: Int) {
        val file = archive.toFile()
        val bytes = file.readBytes()
        var found = -1
        var offset = 0
        while (offset <= bytes.size - CENTRAL_SIGNATURE.size) {
            if (CENTRAL_SIGNATURE.indices.all { bytes[offset + it] == CENTRAL_SIGNATURE[it] }) found++
            if (found == entryIndex) break
            offset++
        }
        val fieldOffset = offset + field
        (0 until 4).forEach { bytes[fieldOffset + it] = (value shr (8 * it)).toByte() }
        file.writeBytes(bytes)
    }

    private companion object {
        val CENTRAL_SIGNATURE = byteArrayOf(0x50, 0x4b, 0x01, 0x02)
        const val CENTRAL_UNCOMPRESSED_SIZE = 24
        const val CENTRAL_COMPRESSED_SIZE = 20
    }
}
