package ru.aokazantsev.devatlas.themes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.themes.domain.ThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.StagingArea
import kotlin.random.Random

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class ThemeStorageImpl(
    platform: PlatformBindings,
    private val fileSystem: FileSystem,
    private val backgroundScope: CoroutineScope,
) : ThemeStorage {

    private val themesRoot = platform.dataDirectory / "themes"
    private val stagingRoot = platform.dataDirectory / "staging"
    private var stagingCleared = false

    override fun newStaging(): StagingArea {
        val directory = stagingRoot / Random.nextLong().toULong().toString(HEX_RADIX)
        fileSystem.createDirectories(directory)
        return StagingArea(directory = directory, archive = directory / "archive.zip", content = directory / "content")
    }

    override fun themeDirectory(themeId: String): Path = themesRoot / themeId

    override suspend fun commit(staging: StagingArea, themeId: String) = withContext(Dispatchers.IO) {
        val target = themeDirectory(themeId)
        val backup = staging.directory / PREVIOUS / themeId
        fileSystem.createDirectories(themesRoot)
        val hadPrevious = fileSystem.exists(target)
        if (hadPrevious) {
            fileSystem.createDirectories(staging.directory / PREVIOUS)
            fileSystem.atomicMove(target, backup)
        }
        try {
            fileSystem.atomicMove(staging.content, target)
        } catch (error: IOException) {
            if (hadPrevious) fileSystem.atomicMove(backup, target)
            throw error
        }
    }

    override suspend fun rollback(staging: StagingArea, themeId: String) = withContext(Dispatchers.IO) {
        val target = themeDirectory(themeId)
        val backup = staging.directory / PREVIOUS / themeId
        fileSystem.deleteRecursively(target, mustExist = false)
        if (fileSystem.exists(backup)) fileSystem.atomicMove(backup, target)
    }

    override fun discard(staging: StagingArea) {
        backgroundScope.launch {
            try {
                fileSystem.deleteRecursively(staging.directory, mustExist = false)
            } catch (_: IOException) {
            }
        }
    }

    override suspend fun deleteTheme(themeId: String) = withContext(Dispatchers.IO) {
        fileSystem.deleteRecursively(themeDirectory(themeId), mustExist = false)
    }

    override fun clearStaging() {
        if (stagingCleared) return
        stagingCleared = true
        try {
            restoreInterruptedCommits()
            fileSystem.deleteRecursively(stagingRoot, mustExist = false)
        } catch (_: IOException) {
        }
    }

    private fun restoreInterruptedCommits() {
        val stagings = fileSystem.listOrNull(stagingRoot).orEmpty()
        for (backup in stagings.flatMap { fileSystem.listOrNull(it / PREVIOUS).orEmpty() }) {
            val target = themeDirectory(backup.name)
            if (!fileSystem.exists(target)) {
                fileSystem.createDirectories(themesRoot)
                fileSystem.atomicMove(backup, target)
            }
        }
    }

    private companion object {
        const val HEX_RADIX = 16
        const val PREVIOUS = "previous"
    }
}
