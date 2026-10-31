package ru.aokazantsev.devatlas.files.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.files.FakePublicFiles
import ru.aokazantsev.devatlas.files.domain.model.PdfDownloadResult
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class KtorPdfDownloaderTest {

    private val publicFiles = FakePublicFiles()
    private val requests = mutableListOf<HttpRequestData>()
    private val pdfBytes = "%PDF-1.7 test".encodeToByteArray()
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(NOW)
    }

    private fun downloader(
        endpoints: NetworkEndpoints = NetworkEndpoints.Production,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = KtorPdfDownloader(
        client = HttpClient(MockEngine { request -> requests += request; handler(request) }) { install(HttpTimeout) },
        endpoints = endpoints,
        publicFiles = publicFiles,
        platform = FakePlatformBindings(),
        clock = clock,
    )

    private fun pdfResponse(scope: MockRequestHandleScope, vararg headers: Pair<String, String>) = scope.respond(
        pdfBytes,
        HttpStatusCode.OK,
        headersOf(*(listOf(HttpHeaders.ContentType to "application/pdf") + headers).map { it.first to listOf(it.second) }.toTypedArray()),
    )

    @Test
    fun savesPdfWithNameFromDownloadAttributeAndReportsProgress() = runTest {
        val progress = mutableListOf<Long>()
        val loader = downloader { pdfResponse(this) }

        val result = loader.download(PdfRequest("https://example.com/a.pdf", fileName = "guide.pdf"), null) { received, _ ->
            progress += received
        }

        val saved = assertIs<PdfDownloadResult.Saved>(result).file
        assertEquals("guide.pdf", saved.name)
        assertEquals(pdfBytes.size.toLong(), saved.sizeBytes)
        assertEquals(NOW, saved.downloadedAtMillis)
        val stored = publicFiles.published().getValue(saved.contentUri)
        assertEquals(pdfBytes.decodeToString(), stored.content.readUtf8())
        assertEquals(pdfBytes.size.toLong(), progress.last())
    }

    @Test
    fun sameNameGetsNumberSuffix() = runTest {
        publicFiles.add("guide.pdf")
        val loader = downloader { pdfResponse(this) }

        val result = loader.download(PdfRequest("https://example.com/a.pdf", fileName = "guide.pdf"), null) { _, _ -> }

        assertEquals("guide (1).pdf", assertIs<PdfDownloadResult.Saved>(result).file.name)
    }

    @Test
    fun replacedFileIsDeletedBeforePublishingSoNameIsKept() = runTest {
        val old = publicFiles.add("guide.pdf")
        val loader = downloader { pdfResponse(this) }

        val result = loader.download(PdfRequest("https://example.com/a.pdf", fileName = "guide.pdf"), old) { _, _ -> }

        assertEquals("guide.pdf", assertIs<PdfDownloadResult.Saved>(result).file.name)
        assertTrue(old !in publicFiles.files)
    }

    @Test
    fun nameComesFromContentDisposition() = runTest {
        val loader = downloader { pdfResponse(this, HttpHeaders.ContentDisposition to "attachment; filename=\"server.pdf\"") }

        val result = loader.download(PdfRequest("https://example.com/get?id=1"), null) { _, _ -> }

        assertEquals("server.pdf", assertIs<PdfDownloadResult.Saved>(result).file.name)
    }

    @Test
    fun errorStatusPageAndOversizeLeaveNoFile() = runTest {
        val notFound = downloader { respond("", HttpStatusCode.NotFound) }
        val page = downloader {
            respond("<html></html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))
        }
        val huge = downloader {
            respond(pdfBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, TOO_LARGE_BYTES.toString()))
        }
        val request = PdfRequest("https://example.com/a.pdf")

        val missing = assertIs<PdfDownloadResult.Failed>(notFound.download(request, null) { _, _ -> })
        assertEquals("Сервер ответил ошибкой 404", missing.message)
        assertIs<PdfDownloadResult.Failed>(page.download(request, null) { _, _ -> })
        assertTrue(assertIs<PdfDownloadResult.Failed>(huge.download(request, null) { _, _ -> }).message.contains("100 МБ"))
        assertTrue(publicFiles.files.isEmpty())
    }

    @Test
    fun bodyWithoutPdfSignatureIsRefusedAndOldFileKept() = runTest {
        val old = publicFiles.add("guide.pdf")
        val loader = downloader {
            respond("PK not a pdf", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/pdf"))
        }

        val result = loader.download(PdfRequest("https://example.com/a.pdf", fileName = "guide.pdf"), old) { _, _ -> }

        assertTrue(assertIs<PdfDownloadResult.Failed>(result).message.startsWith("По ссылке не PDF"))
        assertEquals(setOf(old), publicFiles.files.keys)
    }

    @Test
    fun storageFailureIsReportedAsSaveError() = runTest {
        publicFiles.failCreate = true
        val loader = downloader { pdfResponse(this) }

        val result = loader.download(PdfRequest("https://example.com/a.pdf"), null) { _, _ -> }

        assertTrue(assertIs<PdfDownloadResult.Failed>(result).message.startsWith("Не удалось сохранить файл в Загрузки/DevAtlas"))
    }

    @Test
    fun cleartextIsRefusedWithoutRequest() = runTest {
        val loader = downloader { pdfResponse(this) }

        assertIs<PdfDownloadResult.Failed>(loader.download(PdfRequest("http://example.com/a.pdf"), null) { _, _ -> })
        assertTrue(requests.isEmpty())
    }

    @Test
    fun rewriteSendsRequestToTestServerButKeepsOriginalUrl() = runTest {
        val endpoints = NetworkEndpoints.Production.copy(
            allowCleartext = true,
            webRewrites = mapOf("https://devatlas.test" to "http://127.0.0.1:8080"),
        )
        val loader = downloader(endpoints) { pdfResponse(this) }

        val result = loader.download(PdfRequest("https://devatlas.test/files/guide.pdf"), null) { _, _ -> }

        assertEquals("http://127.0.0.1:8080/files/guide.pdf", requests.single().url.toString())
        assertEquals("https://devatlas.test/files/guide.pdf", assertIs<PdfDownloadResult.Saved>(result).file.url)
    }

    @Test
    fun networkFailureLeavesNoPendingFile() = runTest {
        val loader = downloader { error("сеть пропала") }

        assertIs<PdfDownloadResult.Failed>(loader.download(PdfRequest("https://example.com/a.pdf"), null) { _, _ -> })
        assertTrue(publicFiles.files.isEmpty())
    }

    @Test
    fun failedRedownloadKeepsOldFile() = runTest {
        val old = publicFiles.add("guide.pdf")
        val loader = downloader { respond("", HttpStatusCode.InternalServerError) }

        assertIs<PdfDownloadResult.Failed>(loader.download(PdfRequest("https://example.com/a.pdf"), old) { _, _ -> })

        assertTrue(old in publicFiles.files)
    }

    @Test
    fun writeFailureRemovesPendingAndKeepsOldFile() = runTest {
        val old = publicFiles.add("guide.pdf")
        publicFiles.failWrite = true
        val loader = downloader { pdfResponse(this) }

        val result = loader.download(PdfRequest("https://example.com/a.pdf"), old) { _, _ -> }

        assertTrue(assertIs<PdfDownloadResult.Failed>(result).message.startsWith("Не удалось сохранить файл"))
        assertEquals(setOf(old), publicFiles.files.keys)
    }

    @Test
    fun pdfBuiltInBrowserIsRefusedWithoutRequest() = runTest {
        val loader = downloader { pdfResponse(this) }

        val blob = loader.download(PdfRequest("blob:https://example.com/1"), null) { _, _ -> }
        val data = loader.download(PdfRequest("data:application/pdf;base64,JVBE"), null) { _, _ -> }

        assertTrue(assertIs<PdfDownloadResult.Failed>(blob).message.contains("в браузере"))
        assertIs<PdfDownloadResult.Failed>(data)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun emptyBodyIsNotPdf() = runTest {
        val loader = downloader {
            respond(ByteArray(0), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/pdf"))
        }

        val result = loader.download(PdfRequest("https://example.com/a.pdf"), null) { _, _ -> }

        assertTrue(assertIs<PdfDownloadResult.Failed>(result).message.startsWith("По ссылке не PDF"))
        assertTrue(publicFiles.files.isEmpty())
    }

    @Test
    fun signatureSplitAcrossChunksIsAccepted() = runTest {
        val progress = mutableListOf<Long>()
        val body = ByteChannel(autoFlush = true)
        launch(Dispatchers.Default) {
            body.writeFully(pdfBytes, 0, SPLIT_AT)
            delay(CHUNK_PAUSE_MILLIS)
            body.writeFully(pdfBytes, SPLIT_AT, pdfBytes.size)
            body.flushAndClose()
        }
        val loader = downloader {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/pdf"))
        }

        val result = loader.download(PdfRequest("https://example.com/a.pdf", fileName = "guide.pdf"), null) { received, _ ->
            progress += received
        }

        val saved = assertIs<PdfDownloadResult.Saved>(result).file
        assertTrue(SPLIT_AT.toLong() in progress)
        assertEquals(pdfBytes.decodeToString(), publicFiles.published().getValue(saved.contentUri).content.readUtf8())
    }

    private companion object {
        const val NOW = 1_761_868_800_000L
        const val TOO_LARGE_BYTES = 100L * 1024 * 1024 + 1
        const val SPLIT_AT = 3
        const val CHUNK_PAUSE_MILLIS = 50L
    }
}
