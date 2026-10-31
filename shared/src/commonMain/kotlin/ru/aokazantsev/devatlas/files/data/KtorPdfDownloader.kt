package ru.aokazantsev.devatlas.files.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.IOException
import okio.buffer
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.files.domain.PdfDownloader
import ru.aokazantsev.devatlas.files.domain.PdfFileNames
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile
import ru.aokazantsev.devatlas.files.domain.model.PdfDownloadResult
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PendingPublicFile
import kotlin.time.Clock

@ContributesBinding(AppScope::class)
@Inject
internal class KtorPdfDownloader(
    private val client: HttpClient,
    private val endpoints: NetworkEndpoints,
    private val publicFiles: PublicFiles,
    private val platform: PlatformBindings,
    private val clock: Clock,
) : PdfDownloader {

    override suspend fun download(
        request: PdfRequest,
        replacing: String?,
        onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): PdfDownloadResult {
        val url = endpoints.webUrl(request.url)
        if (url.startsWith(BLOB, ignoreCase = true) || url.startsWith(DATA, ignoreCase = true)) {
            return PdfDownloadResult.Failed("Этот PDF сайт собирает прямо в браузере: откройте страницу в браузере и скачайте его там")
        }
        if (!url.startsWith(HTTPS, ignoreCase = true) && !(endpoints.allowCleartext && url.startsWith(HTTP, ignoreCase = true))) {
            return PdfDownloadResult.Failed("Ссылка без шифрования: PDF скачивается только по https://")
        }
        return try {
            client.prepareGet(url) {
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
            }.execute { response -> receive(request, response, replacing, onProgress) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            PdfDownloadResult.Failed("Не удалось скачать: нет сети или сервер не отвечает. Проверьте интернет и повторите")
        }
    }

    private suspend fun receive(
        request: PdfRequest,
        response: HttpResponse,
        replacing: String?,
        onProgress: (Long, Long?) -> Unit,
    ): PdfDownloadResult {
        val total = response.contentLength()
        return when {
            response.status != HttpStatusCode.OK -> PdfDownloadResult.Failed("Сервер ответил ошибкой ${response.status.value}")
            isPage(response) -> PdfDownloadResult.Failed(
                "По ссылке не PDF, а страница сайта: скопируйте ссылку удержанием и откройте её в браузере",
            )
            total != null && total > MAX_BYTES -> PdfDownloadResult.Failed(TOO_LARGE)
            else -> {
                val now = clock.now().toEpochMilliseconds()
                val name = PdfFileNames.choose(
                    request = request,
                    responseDisposition = response.headers[HttpHeaders.ContentDisposition],
                    finalUrl = response.request.url.toString(),
                    localMillis = now + platform.timeZoneOffsetMillis(now),
                )
                store(request, response, name, total, replacing, onProgress, now)
            }
        }
    }

    private suspend fun store(
        request: PdfRequest,
        response: HttpResponse,
        name: String,
        total: Long?,
        replacing: String?,
        onProgress: (Long, Long?) -> Unit,
        now: Long,
    ): PdfDownloadResult {
        val pending = try {
            publicFiles.create(name, PDF_TYPE)
        } catch (error: IOException) {
            return PdfDownloadResult.Failed(saveFailure(error))
        }
        var saved = false
        try {
            val result = try {
                write(request, pending, response, total, replacing, onProgress, now)
            } catch (error: PublicFileWriteException) {
                PdfDownloadResult.Failed(saveFailure(error.cause))
            } catch (_: TooLargeException) {
                PdfDownloadResult.Failed(TOO_LARGE)
            }
            saved = result is PdfDownloadResult.Saved
            return result
        } finally {
            if (!saved) withContext(NonCancellable) { pending.discardQuietly() }
        }
    }

    private suspend fun write(
        request: PdfRequest,
        pending: PendingPublicFile,
        response: HttpResponse,
        total: Long?,
        replacing: String?,
        onProgress: (Long, Long?) -> Unit,
        now: Long,
    ): PdfDownloadResult {
        val sink = writing { pending.sink().buffer() }
        val header = ByteArray(PDF_SIGNATURE.size)
        val received = try {
            copy(response.bodyAsChannel(), sink, header, total, onProgress)
        } finally {
            writing { sink.close() }
        }
        if (received > MAX_BYTES) throw TooLargeException()
        if (!header.contentEquals(PDF_SIGNATURE)) return PdfDownloadResult.Failed(NOT_PDF)
        currentCoroutineContext().ensureActive()
        replacing?.let(publicFiles::delete)
        val finalName = pending.publish() ?: return PdfDownloadResult.Failed(saveFailure(null))
        return PdfDownloadResult.Saved(
            DownloadedFile(
                url = request.url,
                contentUri = pending.uri,
                name = finalName,
                sizeBytes = received,
                downloadedAtMillis = now,
            ),
        )
    }

    private suspend fun copy(
        channel: ByteReadChannel,
        sink: BufferedSink,
        header: ByteArray,
        total: Long?,
        onProgress: (Long, Long?) -> Unit,
    ): Long {
        val buffer = ByteArray(CHUNK_SIZE)
        var received = 0L
        onProgress(0L, total)
        while (received <= MAX_BYTES) {
            val read = channel.readAvailable(buffer, 0, buffer.size)
            if (read == -1) break
            if (received < header.size) {
                buffer.copyInto(header, received.toInt(), 0, minOf(read, header.size - received.toInt()))
            }
            writing { sink.write(buffer, 0, read) }
            received += read
            onProgress(received, total)
        }
        return received
    }

    private inline fun <T> writing(block: () -> T): T = try {
        block()
    } catch (error: IOException) {
        throw PublicFileWriteException(error)
    }

    private fun isPage(response: HttpResponse): Boolean =
        response.headers[HttpHeaders.ContentType]?.trimStart()?.startsWith(HTML_TYPE, ignoreCase = true) == true

    private fun PendingPublicFile.discardQuietly() {
        try {
            discard()
        } catch (_: IOException) {
        }
    }

    private fun saveFailure(error: Throwable?): String =
        "Не удалось сохранить файл в Загрузки/${publicFiles.folderName}" + (error?.message?.let { ": $it" } ?: "")

    private companion object {
        const val HTTPS = "https://"
        const val HTTP = "http://"
        const val BLOB = "blob:"
        const val DATA = "data:"
        const val PDF_TYPE = "application/pdf"
        const val HTML_TYPE = "text/html"
        const val CHUNK_SIZE = 8_192
        const val MAX_MEGABYTES = 100L
        const val MAX_BYTES = MAX_MEGABYTES * 1024 * 1024
        const val TOO_LARGE = "Файл больше $MAX_MEGABYTES МБ: такие PDF приложение не скачивает"
        const val NOT_PDF = "По ссылке не PDF: сервер прислал файл другого вида. Откройте ссылку в браузере"
        val PDF_SIGNATURE = "%PDF-".encodeToByteArray()
    }
}
