package ru.aokazantsev.devatlas.ui.downloads

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {

    private val publicFiles = FakePublicFiles()
    private val repository = FakeDownloadedFileRepository()
    private val apps = FakeExternalApps()
    private val scopes = ViewModelScopes()

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
        DownloadsViewModel(
            repository = repository,
            publicFiles = publicFiles,
            externalApps = apps,
            pdfFiles = PdfFilesImpl(
                downloader = ScriptedPdfDownloader(publicFiles),
                repository = repository,
                publicFiles = publicFiles,
                externalApps = apps,
                scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
                usage = FakeUsageStatistics(),
                journal = FakeJournal(),
            ),
            platform = FakePlatformBindings().apply { timeZoneOffset = THREE_HOURS },
        ),
    )

    private suspend fun stored(name: String, url: String, readable: Boolean = true): DownloadedFile {
        val uri = publicFiles.add(name)
        if (!readable) publicFiles.unreadable += uri
        return DownloadedFile(url, uri, name, SIZE, DOWNLOADED_AT).also { repository.save(it) }
    }

    private suspend fun DownloadsViewModel.rows(): List<DownloadRow> =
        (state.first { it is DownloadsState.Ready } as DownloadsState.Ready).rows

    @Test
    fun rowsShowSizeLocalDateAndMissingFiles() = runTest {
        stored("a.pdf", "https://example.com/a.pdf")
        stored("b.pdf", "https://example.com/b.pdf", readable = false)

        val rows = viewModel().rows()

        assertEquals("1,5 МБ · 01.11.2025", rows.first { it.file.name == "a.pdf" }.details)
        assertFalse(rows.first { it.file.name == "b.pdf" }.available)
    }

    @Test
    fun shareOfMissingFileExplainsInsteadOfSharing() = runTest {
        stored("b.pdf", "https://example.com/b.pdf", readable = false)
        val viewModel = viewModel()

        viewModel.share(viewModel.rows().single())

        assertTrue(apps.sharedFiles.isEmpty())
        assertNotNull(viewModel.notice.value)
    }

    @Test
    fun shareWithoutAppExplainsAndSharedFileIsPassed() = runTest {
        val file = stored("a.pdf", "https://example.com/a.pdf")
        apps.shares = false
        val viewModel = viewModel()

        viewModel.share(viewModel.rows().single())

        assertEquals(listOf(file.contentUri), apps.sharedFiles)
        assertNotNull(viewModel.notice.value)
    }

    @Test
    fun deleteAllRemovesFilesAndRecords() = runTest {
        stored("a.pdf", "https://example.com/a.pdf")
        stored("b.pdf", "https://example.com/b.pdf")
        val viewModel = viewModel()
        viewModel.rows()

        viewModel.deleteAll()

        repository.files.first { it.isEmpty() }
        assertTrue(publicFiles.files.isEmpty())
    }

    @Test
    fun openOfMissingFileDownloadsItAgain() = runTest {
        stored("b.pdf", "https://example.com/b.pdf", readable = false)
        val viewModel = viewModel()

        viewModel.open(viewModel.rows().single())

        assertEquals(1, apps.openedFiles.size)
        assertTrue(repository.files.value.single().contentUri in publicFiles.files)
    }

    private companion object {
        const val SIZE = 1_572_864L
        const val DOWNLOADED_AT = 1_761_944_400_000L
        const val THREE_HOURS = 10_800_000L
    }
}
