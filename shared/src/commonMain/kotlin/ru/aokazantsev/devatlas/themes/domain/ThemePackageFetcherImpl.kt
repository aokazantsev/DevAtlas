package ru.aokazantsev.devatlas.themes.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.themeformat.archive.ThemeArchiveExtractor
import ru.aokazantsev.devatlas.themeformat.validation.ThemeValidator
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.DownloadResult
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.StagingArea
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.ThemeHelp
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan

@ContributesBinding(AppScope::class)
@Inject
internal class ThemePackageFetcherImpl(
    private val downloader: ThemeDownloader,
    private val extractor: ThemeArchiveExtractor,
    private val validator: ThemeValidator,
    private val storage: ThemeStorage,
    private val reader: ThemePackageReader,
    private val fileSystem: FileSystem,
    private val journal: AppJournal,
) : ThemePackageFetcher {

    override suspend fun fetch(plan: ThemeSourcePlan, validators: CacheValidators?): FetchResult =
        fetchStaged(plan, validators).also { journal.write("theme download: ${plan.address}: ${it.describe()}") }

    private fun FetchResult.describe(): String = when (this) {
        is FetchResult.Fetched -> "ok"
        FetchResult.NotModified -> "not modified"
        is FetchResult.Failed -> "failed: ${failure.message}"
    }

    private suspend fun fetchStaged(plan: ThemeSourcePlan, validators: CacheValidators?): FetchResult = withContext(Dispatchers.IO) {
        val staging = try {
            storage.newStaging()
        } catch (error: IOException) {
            return@withContext FetchResult.Failed(writeFailure(error))
        }
        val result = try {
            fetchInto(staging, plan, validators)
        } catch (cancellation: CancellationException) {
            storage.discard(staging)
            throw cancellation
        } catch (error: IOException) {
            FetchResult.Failed(writeFailure(error))
        }
        if (result !is FetchResult.Fetched) storage.discard(staging)
        result
    }

    private fun writeFailure(error: IOException) =
        ThemeFailure("Не удалось сохранить тему на телефон: ${error.message ?: "не хватает места или ошибка записи"}")

    private suspend fun fetchInto(staging: StagingArea, plan: ThemeSourcePlan, validators: CacheValidators?): FetchResult {
        val downloaded = when (val download = downloader.download(plan, validators, staging.archive)) {
            is DownloadResult.Failed -> return FetchResult.Failed(download.failure)
            DownloadResult.NotModified -> return FetchResult.NotModified
            is DownloadResult.Downloaded -> download
        }
        return unpack(staging, plan, downloaded)
    }

    private fun unpack(staging: StagingArea, plan: ThemeSourcePlan, downloaded: DownloadResult.Downloaded): FetchResult {
        val extraction = extractor.extract(fileSystem, staging.archive, staging.content, plan.subFolder)
        if (!extraction.succeeded) return FetchResult.Failed(ThemeFailure.fromIssues(extraction.issues))
        val check = validator.validate(fileSystem, staging.content)
        val manifest = check.manifest
        if (!check.passed || manifest == null) {
            val errors = check.errors.ifEmpty { check.issues }
            val failure = if (errors.isEmpty()) {
                ThemeFailure("Тема не прошла проверку", ThemeHelp.PAGE_URL)
            } else {
                ThemeFailure.fromIssues(errors)
            }
            return FetchResult.Failed(failure)
        }
        return FetchResult.Fetched(
            PreparedTheme(
                staging = staging,
                manifest = manifest,
                source = plan.address,
                etag = downloaded.etag,
                lastModified = downloaded.lastModified,
                sha256 = reader.contentDigest(staging.content),
                sizeBytes = downloaded.sizeBytes,
            ),
        )
    }
}
