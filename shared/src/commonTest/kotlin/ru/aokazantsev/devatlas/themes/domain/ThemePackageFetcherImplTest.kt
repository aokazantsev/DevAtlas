package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.themeformat.archive.ExtractionResult
import ru.aokazantsev.devatlas.themeformat.archive.ThemeArchiveExtractor
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.validation.ThemeCheckResult
import ru.aokazantsev.devatlas.themeformat.validation.ThemeValidator
import ru.aokazantsev.devatlas.themes.FakePackageReader
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.ArchiveLocation
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.DownloadResult
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan
import ru.aokazantsev.devatlas.themes.manifestOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ThemePackageFetcherImplTest {

    private val storage = FakeThemeStorage()
    private val plan = ThemeSourcePlan(
        address = "https://github.com/o/r/tree/main/themes/a",
        location = ArchiveLocation.Direct("https://codeload.github.com/o/r/zip/main"),
        subFolder = "themes/a",
    )
    private val downloaded = DownloadResult.Downloaded(etag = "\"e\"", lastModified = null, sizeBytes = 42L)
    private val error = ThemeIssue(IssueSeverity.ERROR, IssueCode.PAGE_WRAPPER, "обёртка", file = "a.html", line = 1)

    private var download: suspend () -> DownloadResult = { downloaded }
    private var extraction = ExtractionResult(files = listOf("manifest.json"), issues = emptyList())
    private var check = ThemeCheckResult(manifest = manifestOf("a", "A", "p1"), issues = emptyList())
    private val subFolders = mutableListOf<String?>()

    private val journal = FakeJournal()

    private val fetcher = ThemePackageFetcherImpl(
        downloader = object : ThemeDownloader {
            override suspend fun download(plan: ThemeSourcePlan, validators: CacheValidators?, target: Path) = download()
        },
        extractor = object : ThemeArchiveExtractor {
            override fun extract(fileSystem: FileSystem, archive: Path, target: Path, subFolder: String?): ExtractionResult {
                subFolders += subFolder
                return extraction
            }
        },
        validator = object : ThemeValidator {
            override fun validate(fileSystem: FileSystem, root: Path) = check
        },
        storage = storage,
        reader = FakePackageReader(),
        fileSystem = FakeFileSystem(),
        journal = journal,
    )

    @Test
    fun validThemeIsPreparedWithContentDigestAndSubFolder() = runTest {
        val prepared = assertIs<FetchResult.Fetched>(fetcher.fetch(plan)).prepared

        assertEquals(listOf<String?>("themes/a"), subFolders)
        assertEquals("digest:${prepared.staging.content}", prepared.sha256)
        assertEquals(42L, prepared.sizeBytes)
        assertEquals("\"e\"", prepared.etag)
        assertEquals(plan.address, prepared.source)
        assertTrue(storage.discarded.isEmpty())
        assertEquals(listOf("theme download: ${plan.address}: ok"), journal.lines)
    }

    @Test
    fun notModifiedAndDownloadFailureDiscardStaging() = runTest {
        download = { DownloadResult.NotModified }
        assertEquals(FetchResult.NotModified, fetcher.fetch(plan))

        download = { DownloadResult.Failed(ThemeFailure("нет сети")) }
        assertEquals(FetchResult.Failed(ThemeFailure("нет сети")), fetcher.fetch(plan))

        assertEquals(2, storage.discarded.size)
    }

    @Test
    fun extractionAndValidationErrorsBecomeFailuresAndDiscardStaging() = runTest {
        extraction = ExtractionResult(files = emptyList(), issues = listOf(error))
        assertTrue(assertIs<FetchResult.Failed>(fetcher.fetch(plan)).failure.message.contains("обёртка"))

        extraction = ExtractionResult(files = listOf("manifest.json"), issues = emptyList())
        check = ThemeCheckResult(manifest = null, issues = listOf(error))
        assertTrue(assertIs<FetchResult.Failed>(fetcher.fetch(plan)).failure.message.contains("a.html:1"))

        assertEquals(2, storage.discarded.size)
    }

    @Test
    fun failedCheckWithoutIssuesHasFallbackText() = runTest {
        check = ThemeCheckResult(manifest = null, issues = emptyList())

        assertEquals("Тема не прошла проверку", assertIs<FetchResult.Failed>(fetcher.fetch(plan)).failure.message)
    }

    @Test
    fun cancellationDiscardsStagingAndPropagates() = runTest {
        download = { throw CancellationException("ушли с экрана") }

        assertFailsWith<CancellationException> { fetcher.fetch(plan) }
        assertEquals(1, storage.discarded.size)
    }

    @Test
    fun stagingWriteFailureBecomesFailure() = runTest {
        storage.stagingFailure = IOException("No space left on device")

        val failure = assertIs<FetchResult.Failed>(fetcher.fetch(plan)).failure

        assertEquals("Не удалось сохранить тему на телефон: No space left on device", failure.message)
    }

    @Test
    fun readFailureAfterDownloadBecomesFailureAndDiscardsStaging() = runTest {
        download = { throw IOException("read failed") }

        assertIs<FetchResult.Failed>(fetcher.fetch(plan))
        assertEquals(1, storage.discarded.size)
    }
}
