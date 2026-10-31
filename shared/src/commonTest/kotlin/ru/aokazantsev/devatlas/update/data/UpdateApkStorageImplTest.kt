package ru.aokazantsev.devatlas.update.data

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateApkStorageImplTest {

    private val fileSystem = FakeFileSystem()

    private fun storage(installed: String = "0.2") = UpdateApkStorageImpl(
        fileSystem = fileSystem,
        appInfo = AppInfo(versionName = installed, isDebug = false),
        platform = FakePlatformBindings(),
    )

    private fun put(vararg names: String) {
        fileSystem.createDirectories(UPDATES)
        names.forEach { name -> fileSystem.write(UPDATES / name) { writeUtf8("apk") } }
    }

    private fun left() = fileSystem.list(UPDATES).map { it.name }.sorted()

    @Test
    fun installedAndOlderApksAreRemovedNewerIsKept() {
        put("DevAtlas-0.1.apk", "DevAtlas-0.2.apk", "DevAtlas-0.3.apk")

        storage(installed = "0.2-debug").discardInstalled()

        assertEquals(listOf("DevAtlas-0.3.apk"), left())
    }

    @Test
    fun discardAllRemovesApksButNotOtherFiles() {
        put("DevAtlas-0.3.apk", "note.txt")

        storage().discardAll()

        assertEquals(listOf("note.txt"), left())
    }

    @Test
    fun missingFolderIsNotAnError() {
        storage().discardAll()
        storage().discardInstalled()

        assertTrue(!fileSystem.exists(UPDATES))
    }

    private companion object {
        val UPDATES = "/data/updates".toPath()
    }
}
