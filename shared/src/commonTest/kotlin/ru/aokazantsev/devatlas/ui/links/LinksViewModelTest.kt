package ru.aokazantsev.devatlas.ui.links

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.files.FakeDownloadedFileRepository
import ru.aokazantsev.devatlas.files.FakeExternalApps
import ru.aokazantsev.devatlas.files.FakePublicFiles
import ru.aokazantsev.devatlas.files.ScriptedPdfDownloader
import ru.aokazantsev.devatlas.files.domain.PdfFilesImpl
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PdfState
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LinksViewModelTest {

    private val publicFiles = FakePublicFiles()
    private val repository = FakeDownloadedFileRepository()
    private val apps = FakeExternalApps()
    private val downloader = ScriptedPdfDownloader(publicFiles)
    private val platform = FakePlatformBindings()
    private val scopes = ViewModelScopes()
    private val request = PdfRequest(url = "https://example.com/guide.pdf", fileName = "guide.pdf")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = scopes.track(
        LinksViewModel(
            pdfFiles = PdfFilesImpl(
                downloader = downloader,
                repository = repository,
                publicFiles = publicFiles,
                externalApps = apps,
                scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
                usage = FakeUsageStatistics(),
            ),
            externalApps = apps,
            platform = platform,
            usage = FakeUsageStatistics(),
        ),
    )

    private suspend fun known(): DownloadedFile {
        val uri = publicFiles.add("guide.pdf")
        return DownloadedFile(request.url, uri, "guide.pdf", SIZE, 0L).also { repository.save(it) }
    }

    @Test
    fun retryAfterFailedRedownloadDownloadsAgainReplacingOldFile() = runTest {
        val file = known()
        val viewModel = viewModel()

        viewModel.retryPdf(PdfState.Failed("ошибка", request, redownload = true))

        assertEquals(request to file.contentUri, downloader.requests.single())
    }

    @Test
    fun retryAfterFailedOpenOpensExistingFileWithoutDownload() = runTest {
        val file = known()
        val viewModel = viewModel()

        viewModel.retryPdf(PdfState.Failed("ошибка", request, redownload = false))

        assertTrue(downloader.requests.isEmpty())
        assertEquals(listOf(file.contentUri), apps.openedFiles)
    }

    @Test
    fun linkWithoutAppShowsNoticeUntilDismissed() = runTest {
        apps.opens = false
        val viewModel = viewModel()

        viewModel.openExternal("tel:+70000000000")

        assertEquals(listOf("tel:+70000000000"), apps.openedUris)
        assertNotNull(viewModel.notice.value)
        viewModel.dismissNotice()
        assertNull(viewModel.notice.value)
    }

    @Test
    fun copyLinkPutsUrlToClipboard() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.copyLink(request.url))

        assertEquals(request.url, platform.clipboard)
    }

    @Test
    fun copyLinkShowsOwnHintOnlyWhereSystemDoesNot() = runTest {
        val viewModel = viewModel()

        viewModel.copyLink(request.url)
        assertEquals(0, platform.copiedHints)

        platform.clipboardConfirmsCopy = false
        viewModel.copyLink(request.url)
        assertEquals(1, platform.copiedHints)

        platform.copySucceeds = false
        viewModel.copyLink(request.url)
        assertEquals(1, platform.copiedHints)
    }

    private companion object {
        const val SIZE = 10L
    }
}
