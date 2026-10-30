package ru.aokazantsev.devatlas.themes.data

import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ThemePackageReaderImplTest {

    private val fileSystem = FakeFileSystem()
    private val reader = ThemePackageReaderImpl(fileSystem)

    private val manifest =
        """{ "formatVersion": 1, "id": "demo", "title": "Демо", "tree": [ { "id": "a", "title": "A", "file": "a.html" } ] }"""

    private fun theme(root: String, files: List<Pair<String, String>>): Path {
        val path = root.toPath()
        files.forEach { (name, text) ->
            val file = path / name
            file.parent?.let(fileSystem::createDirectories)
            fileSystem.write(file) { writeUtf8(text) }
        }
        return path
    }

    private val base = listOf("manifest.json" to manifest, "a.html" to "<p>a</p>", "d/x.svg" to "<svg/>")

    @Test
    fun sameFilesWrittenInAnotherOrderGiveSameDigest() {
        val first = theme("/one", base)
        val second = theme("/two", base.reversed())

        assertEquals(reader.contentDigest(first), reader.contentDigest(second))
    }

    @Test
    fun changedSchemeChangesDigestButNotPageHashes() {
        val first = theme("/one", base)
        val second = theme("/two", base.map { if (it.first == "d/x.svg") it.first to "<svg id=\"new\"/>" else it })
        val parsed = reader.manifest(first)!!

        assertNotEquals(reader.contentDigest(first), reader.contentDigest(second))
        assertEquals(reader.pageHashes(first, parsed), reader.pageHashes(second, parsed))
    }

    @Test
    fun renamedFileChangesDigest() {
        val first = theme("/one", base)
        val second = theme("/two", base.map { if (it.first == "d/x.svg") "d/y.svg" to it.second else it })

        assertNotEquals(reader.contentDigest(first), reader.contentDigest(second))
    }

    @Test
    fun hiddenFilesDoNotChangeDigest() {
        val first = theme("/one", base)
        val second = theme("/two", base + (".git/config" to "x") + ("__MACOSX/._a.html" to "y"))

        assertEquals(reader.contentDigest(first), reader.contentDigest(second))
    }

    @Test
    fun manifestWithByteOrderMarkIsRead() {
        val root = theme("/bom", listOf("manifest.json" to "﻿" + manifest))

        assertEquals("demo", reader.manifest(root)?.id)
    }

    @Test
    fun missingManifestIsNull() {
        assertNull(reader.manifest("/nothing".toPath()))
    }
}
