package ru.aokazantsev.devatlas.themes.domain

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.IOException
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.SourceResolution
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.ThemeHelp
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.time.Clock

@ContributesBinding(AppScope::class)
@Inject
@Suppress("LongParameterList")
internal class ThemeInstallerImpl(
    private val resolver: ThemeSourceResolver,
    private val fetcher: ThemePackageFetcher,
    private val storage: ThemeStorage,
    private val repository: InstalledThemeRepository,
    private val progress: ProgressRepository,
    private val current: CurrentThemeRepository,
    private val clock: Clock,
    private val usage: UsageStatistics,
) : ThemeInstaller {

    override suspend fun prepare(address: String): PrepareResult {
        val plan = when (val resolution = resolver.resolve(address)) {
            is SourceResolution.Rejected -> return PrepareResult.Failed(ThemeFailure(resolution.message, ThemeHelp.PUBLISH_URL))
            is SourceResolution.Resolved -> resolution.plan
        }
        return when (val fetched = fetcher.fetch(plan)) {
            is FetchResult.Fetched -> PrepareResult.Ready(fetched.prepared)
            is FetchResult.Failed -> PrepareResult.Failed(fetched.failure)
            FetchResult.NotModified -> PrepareResult.Failed(ThemeFailure("Сервер не отдал тему, попробуйте ещё раз"))
        }
    }

    override suspend fun install(prepared: PreparedTheme): InstallResult = withContext(NonCancellable) {
        try {
            InstallResult.Installed(commit(prepared))
        } catch (error: IOException) {
            storage.discard(prepared.staging)
            InstallResult.Failed(ThemeFailure("Не удалось записать тему на телефон: ${error.message ?: "ошибка записи"}"))
        } catch (error: SQLiteException) {
            storage.discard(prepared.staging)
            InstallResult.Failed(ThemeFailure("Не удалось сохранить тему в базе приложения: ${error.message ?: "ошибка базы"}"))
        }
    }

    private suspend fun commit(prepared: PreparedTheme): InstalledTheme {
        val id = prepared.manifest.id
        val previous = repository.find(id)
        val becomesCurrent = current.currentThemeId.first()?.let { repository.find(it) } == null
        storage.commit(prepared.staging, id)
        val theme = try {
            record(prepared, previous, becomesCurrent)
        } catch (error: SQLiteException) {
            restoreRecord(id, previous)
            storage.rollback(prepared.staging, id)
            throw error
        }
        storage.discard(prepared.staging)
        usage.hit(if (previous == null) UsageEvent.THEME_DOWNLOAD else UsageEvent.THEME_UPDATE)
        return theme
    }

    private suspend fun restoreRecord(id: String, previous: InstalledTheme?) {
        try {
            if (previous == null) repository.delete(id) else repository.save(previous)
        } catch (_: SQLiteException) {
        }
    }

    private suspend fun record(prepared: PreparedTheme, previous: InstalledTheme?, becomesCurrent: Boolean): InstalledTheme {
        val id = prepared.manifest.id
        val now = clock.now().toEpochMilliseconds()
        val theme = InstalledTheme(
            id = id,
            title = prepared.manifest.title,
            description = prepared.manifest.description,
            sourceUrl = prepared.source,
            etag = prepared.etag,
            lastModified = prepared.lastModified,
            sha256 = prepared.sha256,
            checkedAt = now,
            pageCount = prepared.manifest.pages.size,
            sizeBytes = prepared.sizeBytes,
            lastUsedAt = previous?.lastUsedAt ?: if (becomesCurrent) now else NEVER_USED,
        )
        repository.save(theme)
        progress.keepOnly(id, prepared.manifest)
        if (becomesCurrent) current.setCurrent(id)
        return theme
    }

    override fun discard(prepared: PreparedTheme) {
        storage.discard(prepared.staging)
    }

    override suspend fun redownload(themeId: String): ThemeFailure? {
        val theme = repository.find(themeId) ?: return ThemeFailure("Тема уже удалена")
        return when (val result = prepare(theme.sourceUrl)) {
            is PrepareResult.Failed -> result.failure
            is PrepareResult.Ready -> if (result.prepared.manifest.id == themeId) {
                (install(result.prepared) as? InstallResult.Failed)?.failure
            } else {
                discard(result.prepared)
                ThemeFailure(
                    "По ссылке теперь лежит другая тема: «${result.prepared.manifest.title}». " +
                        "Уберите эту тему кнопкой «Выбрать другую тему» и добавьте её заново по новой ссылке",
                )
            }
        }
    }

    private companion object {
        const val NEVER_USED = 0L
    }
}
