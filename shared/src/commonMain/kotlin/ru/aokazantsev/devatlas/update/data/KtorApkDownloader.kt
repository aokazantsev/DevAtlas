package ru.aokazantsev.devatlas.update.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.FileSystem
import okio.HashingSink
import okio.IOException
import okio.Path
import okio.buffer
import ru.aokazantsev.devatlas.app.AppUserAgent
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.update.domain.ApkDownloader
import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.ReleaseApk

@ContributesBinding(AppScope::class)
@Inject
internal class KtorApkDownloader(
    private val client: HttpClient,
    private val endpoints: NetworkEndpoints,
    private val appInfo: AppInfo,
    private val fileSystem: FileSystem,
    platform: PlatformBindings,
) : ApkDownloader {

    private val folder = platform.dataDirectory / UpdateFiles.APK_FOLDER

    override suspend fun download(
        release: AppRelease,
        onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        val apk = release.apk ?: return@withContext ApkDownloadResult.Failed(NO_APK, retryable = false)
        val expected = apk.sha256 ?: return@withContext ApkDownloadResult.Failed(NO_CHECKSUM, retryable = false)
        if (!apk.url.startsWith(HTTPS, ignoreCase = true) && !(endpoints.allowCleartext && apk.url.startsWith(HTTP, ignoreCase = true))) {
            return@withContext ApkDownloadResult.Failed(NO_APK, retryable = false)
        }
        val target = folder / UpdateFiles.apkName(release.version)
        try {
            writing { clearFolder() }
            client.prepareGet(apk.url) {
                header(HttpHeaders.UserAgent, AppUserAgent.of(appInfo))
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
            }.execute { response -> receive(response, apk, expected, target, onProgress) }
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) { deleteQuietly(target) }
            throw cancellation
        } catch (_: ApkWriteException) {
            deleteQuietly(target)
            ApkDownloadResult.Failed(WRITE_FAILED)
        } catch (_: Exception) {
            deleteQuietly(target)
            ApkDownloadResult.Failed(NETWORK_FAILED)
        }
    }

    private suspend fun receive(
        response: HttpResponse,
        apk: ReleaseApk,
        expected: String,
        target: Path,
        onProgress: (Long, Long?) -> Unit,
    ): ApkDownloadResult {
        if (response.status != HttpStatusCode.OK) return ApkDownloadResult.Failed("GitHub ответил ошибкой ${response.status.value}")
        val total = response.contentLength() ?: apk.sizeBytes.takeIf { it > 0 }
        if (total != null && total > MAX_BYTES) return ApkDownloadResult.Failed(TOO_LARGE, retryable = false)
        val hashing = writing { HashingSink.sha256(fileSystem.sink(target)) }
        val sink = hashing.buffer()
        val received = try {
            copy(response, sink, total, onProgress)
        } finally {
            writing { sink.close() }
        }
        val failure = when {
            received > MAX_BYTES -> TOO_LARGE
            hashing.hash.hex() != expected -> CHECKSUM_MISMATCH
            else -> null
        }
        if (failure == null) return ApkDownloadResult.Saved(target)
        deleteQuietly(target)
        return ApkDownloadResult.Failed(failure, retryable = failure != TOO_LARGE)
    }

    private suspend fun copy(response: HttpResponse, sink: BufferedSink, total: Long?, onProgress: (Long, Long?) -> Unit): Long {
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(CHUNK_SIZE)
        var received = 0L
        onProgress(0L, total)
        var read = channel.readAvailable(buffer, 0, buffer.size)
        while (read != -1 && received + read <= MAX_BYTES) {
            writing { sink.write(buffer, 0, read) }
            received += read
            onProgress(received, total)
            read = channel.readAvailable(buffer, 0, buffer.size)
        }
        return if (read == -1) received else received + read
    }

    private fun clearFolder() {
        if (fileSystem.exists(folder)) {
            fileSystem.list(folder)
                .filter { it.name.endsWith(UpdateFiles.APK_EXTENSION) }
                .forEach { fileSystem.delete(it, mustExist = false) }
        }
        fileSystem.createDirectories(folder)
    }

    private inline fun <T> writing(block: () -> T): T = try {
        block()
    } catch (error: IOException) {
        throw ApkWriteException(error)
    }

    private fun deleteQuietly(path: Path) {
        try {
            fileSystem.delete(path, mustExist = false)
        } catch (_: IOException) {
        }
    }

    private companion object {
        const val HTTPS = "https://"
        const val HTTP = "http://"
        const val CHUNK_SIZE = 8_192
        const val MAX_MEGABYTES = 200L
        const val MAX_BYTES = MAX_MEGABYTES * 1024 * 1024
        const val NO_APK = "В этой версии на GitHub нет файла APK: скачайте её со страницы выпусков"
        const val NO_CHECKSUM = "GitHub не прислал контрольную сумму файла: скачайте версию со страницы выпусков"
        const val NETWORK_FAILED = "Не удалось скачать обновление: нет сети или GitHub не отвечает. Проверьте интернет и повторите"
        const val WRITE_FAILED = "Не удалось сохранить обновление на телефон: проверьте свободное место"
        const val TOO_LARGE = "Файл обновления больше $MAX_MEGABYTES МБ: скачайте версию со страницы выпусков"
        const val CHECKSUM_MISMATCH = "Скачанный файл не совпал с опубликованным на GitHub: установка отменена. Повторите позже"
    }
}
