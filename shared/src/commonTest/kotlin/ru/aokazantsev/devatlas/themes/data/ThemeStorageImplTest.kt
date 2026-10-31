package ru.aokazantsev.devatlas.themes.data

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase
import ru.aokazantsev.devatlas.di.DeviceInfo
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeStorageImplTest {

    private val fileSystem = FakeFileSystem()

    private val platform = object : PlatformBindings {
        override val dataDirectory: Path = "/data".toPath()
        override val fileSystem: FileSystem = this@ThemeStorageImplTest.fileSystem

        override fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase> = error("не используется")

        override fun httpClient(): HttpClient = error("не используется")

        override fun clipboardText(): String? = null

        override val publicFiles: PublicFiles get() = error("не используется")
        override val externalApps: ExternalApps get() = error("не используется")
        override val appInstaller: AppInstaller get() = error("не используется")
        override val usageScheduler: UsageScheduler get() = error("не используется")
        override val device: DeviceInfo = DeviceInfo(model = "Pixel 8", androidVersion = "14")

        override val clipboardConfirmsCopy: Boolean = true

        override fun copyToClipboard(text: String): Boolean = error("не используется")

        override fun showCopiedHint() = error("не используется")

        override fun timeZoneOffsetMillis(epochMillis: Long): Long = 0L
    }

    private val storage = ThemeStorageImpl(platform, fileSystem, CoroutineScope(Dispatchers.Unconfined))

    private fun stageContent(text: String) = storage.newStaging().also { staging ->
        fileSystem.createDirectories(staging.content)
        fileSystem.write(staging.content / "manifest.json") { writeUtf8(text) }
    }

    @Test
    fun commitMovesContentIntoThemeFolder() = runTest {
        val staging = stageContent("new")

        storage.commit(staging, "a")

        assertEquals("new", fileSystem.read(storage.themeDirectory("a") / "manifest.json") { readUtf8() })
        assertFalse(fileSystem.exists(staging.content))
    }

    @Test
    fun rollbackReturnsPreviousVersion() = runTest {
        storage.commit(stageContent("old"), "a")
        val second = stageContent("new")
        storage.commit(second, "a")

        storage.rollback(second, "a")

        assertEquals("old", fileSystem.read(storage.themeDirectory("a") / "manifest.json") { readUtf8() })
    }

    @Test
    fun rollbackOfFirstInstallRemovesTheme() = runTest {
        val staging = stageContent("new")
        storage.commit(staging, "a")

        storage.rollback(staging, "a")

        assertFalse(fileSystem.exists(storage.themeDirectory("a")))
    }

    @Test
    fun commitReplacesPreviousVersionWholly() = runTest {
        val first = stageContent("old")
        fileSystem.write(first.content / "extra.html") { writeUtf8("x") }
        storage.commit(first, "a")

        storage.commit(stageContent("new"), "a")

        assertEquals("new", fileSystem.read(storage.themeDirectory("a") / "manifest.json") { readUtf8() })
        assertFalse(fileSystem.exists(storage.themeDirectory("a") / "extra.html"))
    }

    @Test
    fun commitKeepsPreviousVersionWhenMoveFails() = runTest {
        storage.commit(stageContent("old"), "a")
        val broken = storage.newStaging()

        runCatching { storage.commit(broken, "a") }

        assertEquals("old", fileSystem.read(storage.themeDirectory("a") / "manifest.json") { readUtf8() })
    }

    @Test
    fun discardRemovesStagingInBackground() {
        val staging = stageContent("x")

        storage.discard(staging)

        assertFalse(fileSystem.exists(staging.directory))
    }

    @Test
    fun stagingIsClearedOnlyOncePerStorage() = runTest {
        storage.clearStaging()
        val fresh = stageContent("идёт установка")

        storage.clearStaging()

        assertTrue(fileSystem.exists(fresh.content / "manifest.json"))
    }

    @Test
    fun deleteAndClearRemoveFolders() = runTest {
        storage.commit(stageContent("x"), "a")
        val pending = stageContent("y")

        storage.deleteTheme("a")
        storage.clearStaging()

        assertFalse(fileSystem.exists(storage.themeDirectory("a")))
        assertFalse(fileSystem.exists(pending.directory))
    }

    @Test
    fun clearStagingReturnsPreviousVersionWhenCommitWasInterrupted() = runTest {
        storage.commit(stageContent("old"), "a")
        val interrupted = stageContent("new")
        fileSystem.createDirectories(interrupted.directory / "previous")
        fileSystem.atomicMove(storage.themeDirectory("a"), interrupted.directory / "previous" / "a")

        ThemeStorageImpl(platform, fileSystem, CoroutineScope(Dispatchers.Unconfined)).clearStaging()

        assertEquals("old", fileSystem.read(storage.themeDirectory("a") / "manifest.json") { readUtf8() })
        assertFalse(fileSystem.exists(interrupted.directory))
    }

    @Test
    fun clearStagingDropsPreviousVersionWhenCommitFinished() = runTest {
        storage.commit(stageContent("old"), "a")
        val finished = stageContent("new")
        storage.commit(finished, "a")

        ThemeStorageImpl(platform, fileSystem, CoroutineScope(Dispatchers.Unconfined)).clearStaging()

        assertEquals("new", fileSystem.read(storage.themeDirectory("a") / "manifest.json") { readUtf8() })
        assertFalse(fileSystem.exists(finished.directory))
    }

    @Test
    fun stagingCleanupFailureDoesNotBreakStart() {
        stageContent("остаток")
        val lockedDisk = object : ForwardingFileSystem(fileSystem) {
            override fun delete(path: Path, mustExist: Boolean) = throw IOException("файл занят")
        }

        ThemeStorageImpl(platform, lockedDisk, CoroutineScope(Dispatchers.Unconfined)).clearStaging()
    }
}
