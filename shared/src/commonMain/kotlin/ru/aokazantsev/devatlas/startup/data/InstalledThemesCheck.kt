package ru.aokazantsev.devatlas.startup.data

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import ru.aokazantsev.devatlas.startup.domain.StartupCheck
import ru.aokazantsev.devatlas.startup.domain.VerifiedManifests
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themes.domain.CurrentThemeRepository
import ru.aokazantsev.devatlas.themes.domain.InstalledThemeRepository
import ru.aokazantsev.devatlas.themes.domain.ThemePackageReader
import ru.aokazantsev.devatlas.themes.domain.ThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.DamagedTheme
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

@ContributesIntoSet(AppScope::class)
@Inject
internal class InstalledThemesCheck(
    private val repository: InstalledThemeRepository,
    private val current: CurrentThemeRepository,
    private val storage: ThemeStorage,
    private val reader: ThemePackageReader,
    private val fileSystem: FileSystem,
    private val verified: VerifiedManifests,
) : StartupCheck {

    override suspend fun check(): StartupCheckResult = try {
        withContext(Dispatchers.IO) { checkThemes() }
    } catch (error: SQLiteException) {
        StartupCheckResult.Failed(error.message ?: "SQLiteException")
    } catch (error: IOException) {
        StartupCheckResult.Failed(error.message ?: "IOException")
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: IllegalStateException) {
        StartupCheckResult.Failed(error.message ?: "IllegalStateException")
    }

    private suspend fun checkThemes(): StartupCheckResult {
        storage.clearStaging()
        val currentId = current.currentThemeId.first()
        val themes = repository.all().sortedByDescending { it.id == currentId }
        val manifests = mutableMapOf<String, ThemeManifest>()
        for (theme in themes) {
            when (val verdict = verify(theme)) {
                is ThemeVerdict.Damaged -> return StartupCheckResult.ThemeDamaged(DamagedTheme(theme.id, theme.title, verdict.reason))
                is ThemeVerdict.Healthy -> manifests[theme.id] = verdict.manifest
            }
        }
        verified.replace(manifests)
        return StartupCheckResult.Passed
    }

    private fun verify(theme: InstalledTheme): ThemeVerdict {
        val directory = storage.themeDirectory(theme.id)
        if (!fileSystem.exists(directory / ThemeManifest.FILE_NAME)) return ThemeVerdict.Damaged("Файлы темы пропали с устройства")
        val manifest = reader.manifest(directory) ?: return ThemeVerdict.Damaged("Файл manifest.json темы испорчен")
        val missing = manifest.pages.firstOrNull { !fileSystem.exists(directory / it.file) }
        return when {
            manifest.id != theme.id -> ThemeVerdict.Damaged("Файлы темы относятся к другой теме")
            missing != null -> ThemeVerdict.Damaged("Пропал файл страницы «${missing.file}»")
            else -> ThemeVerdict.Healthy(manifest)
        }
    }
}
