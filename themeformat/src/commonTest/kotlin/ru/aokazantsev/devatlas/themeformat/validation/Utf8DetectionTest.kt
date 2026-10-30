package ru.aokazantsev.devatlas.themeformat.validation

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Utf8DetectionTest {

    private val fileSystem = FakeFileSystem()
    private val root = "/theme".toPath()

    private val manifest =
        """{ "formatVersion": 1, "id": "demo", "title": "Демо", "tree": [ { "id": "a", "title": "A", "file": "a.html" } ] }"""

    private fun theme(page: ByteArray) {
        fileSystem.createDirectories(root)
        fileSystem.write(root / "manifest.json") { writeUtf8(manifest) }
        fileSystem.write(root / "a.html") { write(page) }
    }

    private fun validate() = ThemeValidatorImpl().validate(fileSystem, root)

    @Test
    fun legitimateReplacementCharacterIsNotAnEncodingError() {
        theme("<p>Символ замены: �</p>".encodeToByteArray())

        val result = validate()

        assertTrue(result.passed, result.issues.toString())
    }

    @Test
    fun windows1251BytesAreStillAnEncodingError() {
        theme("<p>".encodeToByteArray() + byteArrayOf(0xCF.toByte(), 0xF0.toByte()) + "</p>".encodeToByteArray())

        val result = validate()

        assertEquals(listOf(IssueCode.ENCODING), result.issues.map { it.code })
    }

    @Test
    fun truncatedMultibyteSequenceIsAnEncodingError() {
        theme("<p>".encodeToByteArray() + byteArrayOf(0xD0.toByte()))

        assertEquals(IssueCode.ENCODING, validate().issues.first().code)
    }

    @Test
    fun partialManifestDoesNotPassAndIsNotExposed() {
        fileSystem.createDirectories(root)
        fileSystem.write(root / "manifest.json") {
            writeUtf8(
                """{ "formatVersion": 1, "id": "demo", "title": "Д", "tree": [ { "id": "x", "title": "X" }, """ +
                    """{ "id": "a", "title": "A", "file": "a.html" } ] }""",
            )
        }
        fileSystem.write(root / "a.html") { writeUtf8("<p>a</p>") }

        val result = validate()

        assertEquals(false, result.passed)
        assertEquals(null, result.manifest)
    }

    @Test
    fun byteOrderMarkIsStillAccepted() {
        theme(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "<p>BOM</p>".encodeToByteArray())

        assertTrue(validate().passed)
    }
}
