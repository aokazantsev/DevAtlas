package ru.aokazantsev.devatlas.files.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.files.FakeDownloadedFileRepository
import ru.aokazantsev.devatlas.files.FakeExternalApps
import ru.aokazantsev.devatlas.files.FakePublicFiles
import ru.aokazantsev.devatlas.files.GatedPdfDownloader
import ru.aokazantsev.devatlas.files.ScriptedPdfDownloader
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile
import ru.aokazantsev.devatlas.files.domain.model.PdfDownloadResult
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PdfState
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PdfFilesImplTest {

    private val publicFiles = FakePublicFiles()
    private val downloader = ScriptedPdfDownloader(publicFiles)
    private val repository = FakeDownloadedFileRepository()
    private val apps = FakeExternalApps()
    private val request = PdfRequest(url = "https://example.com/guide.pdf", fileName = "guide.pdf")

    private val usage = FakeUsageStatistics()

    private fun TestScope.files() = PdfFilesImpl(
        downloader = downloader,
        repository = repository,
        publicFiles = publicFiles,
        externalApps = apps,
        scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        usage = usage,
        journal = FakeJournal(),
    )

    private suspend fun known(): DownloadedFile {
        val uri = publicFiles.add("guide.pdf")
        return DownloadedFile(request.url, uri, "guide.pdf", SIZE, 0L).also { repository.save(it) }
    }

    @Test
    fun knownReadableFileOpensWithoutDownload() = runTest {
        val file = known()
        val files = files()

        files.open(request)

        assertTrue(downloader.requests.isEmpty())
        assertEquals(listOf(file.contentUri), apps.openedFiles)
        assertEquals(PdfState.Idle, files.state.value)
    }

    @Test
    fun missingFileIsDownloadedRecordedAndOpened() = runTest {
        val file = known()
        publicFiles.unreadable += file.contentUri
        val files = files()

        files.open(request)

        assertEquals(file.contentUri, downloader.requests.single().second)
        val saved = repository.files.value.single()
        assertEquals(listOf(saved.contentUri), apps.openedFiles)
        assertTrue(saved.contentUri != file.contentUri)
    }

    @Test
    fun redownloadIgnoresExistingFileAndReplacesIt() = runTest {
        val file = known()
        val files = files()

        files.redownload(request)

        assertEquals(request to file.contentUri, downloader.requests.single())
        assertEquals("guide.pdf", repository.files.value.single().name)
    }

    @Test
    fun failureIsShownAndCanBeDismissed() = runTest {
        downloader.failure = "Сервер ответил ошибкой 500"
        val files = files()

        files.redownload(request)

        val failed = assertIs<PdfState.Failed>(files.state.value)
        assertEquals("Сервер ответил ошибкой 500", failed.message)
        assertTrue(failed.redownload)
        files.dismiss()
        assertEquals(PdfState.Idle, files.state.value)
    }

    @Test
    fun noViewerIsReportedWithFileName() = runTest {
        apps.opens = false
        val files = files()

        files.open(request)

        assertEquals(PdfState.NoViewer("guide.pdf"), files.state.value)
    }

    @Test
    fun downloadFinishedInBackgroundWaitsForReturnInsteadOfOpening() = runTest {
        apps.foreground = false
        val files = files()

        files.redownload(request)

        val ready = assertIs<PdfState.Ready>(files.state.value)
        assertTrue(apps.openedFiles.isEmpty())
        apps.foreground = true
        files.openSaved(ready.file)
        assertEquals(listOf(ready.file.contentUri), apps.openedFiles)
        assertEquals(PdfState.Idle, files.state.value)
    }

    @Test
    fun savedFileWithoutViewerAfterReturnIsReported() = runTest {
        apps.foreground = false
        val files = files()
        files.redownload(request)
        val ready = assertIs<PdfState.Ready>(files.state.value)
        apps.opens = false

        files.openSaved(ready.file)

        assertEquals(PdfState.NoViewer(ready.file.name), files.state.value)
    }

    private fun TestScope.gatedFiles(gated: GatedPdfDownloader) = PdfFilesImpl(
        downloader = gated,
        repository = repository,
        publicFiles = publicFiles,
        externalApps = apps,
        scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
        usage = usage,
        journal = FakeJournal(),
    )

    private fun saved(uri: String) = PdfDownloadResult.Saved(DownloadedFile(request.url, uri, "guide.pdf", SIZE, 0L))

    @Test
    fun lateProgressAfterCancelDoesNotBringSheetBack() = runTest {
        val gated = GatedPdfDownloader()
        val files = gatedFiles(gated)
        files.open(request)
        assertIs<PdfState.Downloading>(files.state.value)

        files.cancel()
        gated.progress?.invoke(5L, SIZE)

        assertEquals(PdfState.Idle, files.state.value)
    }

    @Test
    fun secondClickDuringDownloadIsIgnored() = runTest {
        val gated = GatedPdfDownloader()
        val files = gatedFiles(gated)

        files.open(request)
        files.open(request)

        assertEquals(1, gated.requests.size)
    }

    @Test
    fun cancelledDownloadNeitherOpensNorOverwritesNextOne() = runTest {
        val gated = GatedPdfDownloader()
        val files = gatedFiles(gated)
        files.open(request)
        val oldProgress = gated.progress
        files.cancel()

        files.open(request)
        oldProgress?.invoke(1L, SIZE)
        assertEquals(2, gated.requests.size)
        assertIs<PdfState.Downloading>(files.state.value)
        gated.gate.complete(saved(publicFiles.add("guide.pdf")))

        assertEquals(1, apps.openedFiles.size)
        assertEquals(PdfState.Idle, files.state.value)
    }

    @Test
    fun cancelBeforeResultKeepsFileUnopened() = runTest {
        val gated = GatedPdfDownloader()
        val files = gatedFiles(gated)
        files.open(request)

        files.cancel()
        gated.gate.complete(saved(publicFiles.add("guide.pdf")))

        assertTrue(apps.openedFiles.isEmpty())
        assertEquals(PdfState.Idle, files.state.value)
    }

    private companion object {
        const val SIZE = 10L
    }
}
