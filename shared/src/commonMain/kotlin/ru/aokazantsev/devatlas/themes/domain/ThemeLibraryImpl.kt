package ru.aokazantsev.devatlas.themes.domain

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.IOException
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.startup.domain.VerifiedManifests
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import kotlin.time.Clock

@ContributesBinding(AppScope::class)
@Inject
internal class ThemeLibraryImpl(
    private val repository: InstalledThemeRepository,
    private val current: CurrentThemeRepository,
    private val storage: ThemeStorage,
    private val progress: ProgressRepository,
    private val clock: Clock,
    private val journal: AppJournal,
    private val verified: VerifiedManifests,
) : ThemeLibrary {

    override fun observeInstalled(): Flow<List<InstalledTheme>> = repository.observeAll()

    override fun observeCurrent(): Flow<InstalledTheme?> =
        combine(current.currentThemeId, repository.observeAll()) { id, all -> all.firstOrNull { it.id == id } }

    override suspend fun resolveCurrent(): InstalledTheme? {
        val id = current.currentThemeId.first()
        val all = repository.all()
        val found = all.firstOrNull { it.id == id }
        if (found != null) return found
        val fallback = all.firstOrNull()
        current.setCurrent(fallback?.id)
        return fallback
    }

    override suspend fun apply(themeId: String) {
        repository.touch(themeId, clock.now().toEpochMilliseconds())
        current.setCurrent(themeId)
    }

    override suspend fun viewedCount(themeId: String): Int = progress.viewedCount(themeId)

    override suspend fun bookmarkCount(themeId: String): Int = progress.bookmarkCount(themeId)

    override suspend fun remove(themeId: String, deleteTheme: Boolean, resetProgress: Boolean): ThemeRemoval =
        withContext(NonCancellable) {
            try {
                removeNow(themeId, deleteTheme, resetProgress)
            } catch (error: SQLiteException) {
                journal.write("theme delete: $themeId: failed: ${error.message}")
                ThemeRemoval.Failed(ThemeFailure("Не удалось изменить базу приложения: ${error.message ?: "ошибка базы"}"))
            }
        }

    private suspend fun removeNow(themeId: String, deleteTheme: Boolean, resetProgress: Boolean): ThemeRemoval {
        if (!deleteTheme) {
            if (resetProgress) progress.reset(themeId)
            return ThemeRemoval.ProgressReset
        }
        val wasCurrent = current.currentThemeId.first() == themeId
        repository.delete(themeId)
        verified.forget(themeId)
        journal.write("theme delete: $themeId: ok")
        if (resetProgress) resetAfterDeleting(themeId)
        try {
            storage.deleteTheme(themeId)
        } catch (_: IOException) {
        }
        return if (wasCurrent) switchAfterDeletingCurrent() else ThemeRemoval.ThemeDeleted
    }

    private suspend fun resetAfterDeleting(themeId: String) {
        try {
            progress.reset(themeId)
        } catch (error: SQLiteException) {
            journal.write("progress reset: $themeId: failed after delete: ${error.message}")
        }
    }

    private suspend fun switchAfterDeletingCurrent(): ThemeRemoval {
        val next = repository.all().firstOrNull()
        if (next == null) {
            current.setCurrent(null)
            return ThemeRemoval.NoThemesLeft
        }
        apply(next.id)
        return ThemeRemoval.SwitchedTo(next.id)
    }
}
