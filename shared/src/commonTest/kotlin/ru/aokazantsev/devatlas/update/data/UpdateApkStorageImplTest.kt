package ru.aokazantsev.devatlas.update.data

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateApkStorageImplTest {

    private val fileSystem = FakeFileSystem()

    private fun storage() = UpdateApkStorageImpl(
        fileSystem = fileSystem,
        platform = FakePlatformBindings(),
    )

    private fun put(vararg names: String) {
        fileSystem.createDirectories(UPDATES)
        names.forEach { name -> fileSystem.write(UPDATES / name) { writeUtf8("apk") } }
    }

    private fun left() = fileSystem.list(UPDATES).map { it.name }.sorted()

    @Test
    fun discardAllRemovesApksButNotOtherFiles() {
        put("DevAtlas-0.3.apk", "note.txt")

        storage().discardAll()

        assertEquals(listOf("note.txt"), left())
    }

    @Test
    fun missingFolderIsNotAnError() {
        storage().discardAll()

        assertTrue(!fileSystem.exists(UPDATES))
    }

    private companion object {
        val UPDATES = "/data/updates".toPath()
    }
}
