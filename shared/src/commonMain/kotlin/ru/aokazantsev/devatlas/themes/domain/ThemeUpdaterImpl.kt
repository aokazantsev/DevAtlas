package ru.aokazantsev.devatlas.themes.domain

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.SourceResolution
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.ThemeHelp
import ru.aokazantsev.devatlas.themes.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.themes.domain.model.UpdateSummary

@ContributesBinding(AppScope::class)
@Inject
internal class ThemeUpdaterImpl(
    private val resolver: ThemeSourceResolver,
    private val fetcher: ThemePackageFetcher,
    private val storage: ThemeStorage,
    private val reader: ThemePackageReader,
    private val repository: InstalledThemeRepository,
    private val journal: AppJournal,
) : ThemeUpdater {

    override suspend fun check(themeId: String, newAddress: String?): UpdateCheck {
        val theme = try {
            repository.find(themeId)
        } catch (error: SQLiteException) {
            journal.write("theme update: $themeId: failed: ${error.message}")
            return UpdateCheck.Failed(ThemeFailure("Не удалось прочитать базу приложения: ${error.message ?: "ошибка базы"}"))
        } ?: return UpdateCheck.Failed(ThemeFailure("Тема не найдена: возможно, её уже удалили"))
        return check(theme, newAddress)
    }

    private suspend fun check(theme: InstalledTheme, newAddress: String?): UpdateCheck {
        val address = newAddress?.takeIf { it.isNotBlank() } ?: theme.sourceUrl
        val plan = when (val resolution = resolver.resolve(address)) {
            is SourceResolution.Rejected -> {
                journal.write("theme update: $address: rejected: ${resolution.message}")
                return UpdateCheck.Failed(ThemeFailure(resolution.message, ThemeHelp.PUBLISH_URL))
            }
            is SourceResolution.Resolved -> resolution.plan
        }
        val validators = if (newAddress == null) CacheValidators(theme.etag, theme.lastModified).takeUnless { it.isEmpty } else null
        return when (val fetched = fetcher.fetch(plan, validators)) {
            FetchResult.NotModified -> UpdateCheck.NoChanges
            is FetchResult.Failed -> UpdateCheck.Failed(fetched.failure)
            is FetchResult.Fetched -> judgeOrDiscard(theme, fetched.prepared, sourceChanged = newAddress != null)
        }
    }

    private suspend fun judgeOrDiscard(theme: InstalledTheme, prepared: PreparedTheme, sourceChanged: Boolean): UpdateCheck = try {
        judge(theme, prepared, sourceChanged)
    } catch (cancellation: CancellationException) {
        storage.discard(prepared.staging)
        throw cancellation
    }

    private suspend fun judge(theme: InstalledTheme, prepared: PreparedTheme, sourceChanged: Boolean): UpdateCheck = when {
        prepared.manifest.id != theme.id -> {
            storage.discard(prepared.staging)
            journal.write("theme update: ${prepared.source}: refused: another theme ${prepared.manifest.id} instead of ${theme.id}")
            UpdateCheck.Failed(
                ThemeFailure(
                    "По этой ссылке другая тема: «${prepared.manifest.title}» (id ${prepared.manifest.id}). " +
                        "Тема «${theme.title}» не изменена: подмена другой темой запрещена, чтобы не потерять прогресс",
                    ThemeHelp.PUBLISH_URL,
                ),
            )
        }
        !sourceChanged && prepared.sha256 == theme.sha256 -> {
            storage.discard(prepared.staging)
            rememberValidators(theme, prepared)
            UpdateCheck.NoChanges
        }
        else -> UpdateCheck.Available(
            prepared = prepared,
            summary = withContext(Dispatchers.IO) { summarize(theme, prepared) },
            contentChanged = prepared.sha256 != theme.sha256,
        )
    }

    private suspend fun rememberValidators(theme: InstalledTheme, prepared: PreparedTheme) {
        if (prepared.etag == theme.etag && prepared.lastModified == theme.lastModified) return
        try {
            repository.updateValidators(theme.id, theme.sha256, prepared.etag, prepared.lastModified)
        } catch (error: SQLiteException) {
            journal.write("theme update: ${theme.id}: validators not saved: ${error.message}")
        }
    }

    private fun summarize(theme: InstalledTheme, prepared: PreparedTheme): UpdateSummary {
        val installedDirectory = storage.themeDirectory(theme.id)
        val oldManifest = reader.manifest(installedDirectory)
        val oldHashes = oldManifest?.let { reader.pageHashes(installedDirectory, it) }.orEmpty()
        val newHashes = reader.pageHashes(prepared.staging.content, prepared.manifest)
        val newSections = prepared.manifest.sectionIds().toSet()
        return UpdateSummary(
            added = newHashes.keys.count { it !in oldHashes },
            removed = oldHashes.keys.count { it !in newHashes },
            changed = newHashes.count { (id, hash) -> oldHashes[id]?.let { it != hash } == true },
            removedSections = oldManifest?.sectionIds().orEmpty().count { it !in newSections },
        )
    }
}
