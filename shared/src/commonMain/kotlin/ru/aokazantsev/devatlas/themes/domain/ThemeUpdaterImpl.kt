package ru.aokazantsev.devatlas.themes.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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
) : ThemeUpdater {

    override suspend fun check(themeId: String, newAddress: String?): UpdateCheck {
        val theme = repository.find(themeId) ?: return UpdateCheck.Failed(ThemeFailure("Тема не найдена: возможно, её уже удалили"))
        val address = newAddress?.takeIf { it.isNotBlank() } ?: theme.sourceUrl
        val plan = when (val resolution = resolver.resolve(address)) {
            is SourceResolution.Rejected -> return UpdateCheck.Failed(ThemeFailure(resolution.message, ThemeHelp.PUBLISH_URL))
            is SourceResolution.Resolved -> resolution.plan
        }
        val validators = if (newAddress == null) CacheValidators(theme.etag, theme.lastModified).takeUnless { it.isEmpty } else null
        return when (val fetched = fetcher.fetch(plan, validators)) {
            FetchResult.NotModified -> UpdateCheck.NoChanges
            is FetchResult.Failed -> UpdateCheck.Failed(fetched.failure)
            is FetchResult.Fetched -> judge(theme, fetched.prepared, sourceChanged = newAddress != null)
        }
    }

    private suspend fun judge(theme: InstalledTheme, prepared: PreparedTheme, sourceChanged: Boolean): UpdateCheck = when {
        prepared.manifest.id != theme.id -> {
            storage.discard(prepared.staging)
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
            UpdateCheck.NoChanges
        }
        else -> UpdateCheck.Available(
            prepared = prepared,
            summary = withContext(Dispatchers.IO) { summarize(theme, prepared) },
            contentChanged = prepared.sha256 != theme.sha256,
        )
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
