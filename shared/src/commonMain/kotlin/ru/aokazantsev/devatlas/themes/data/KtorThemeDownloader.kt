package ru.aokazantsev.devatlas.themes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.BufferedSink
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import okio.use
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themeformat.archive.ArchiveLimits
import ru.aokazantsev.devatlas.themes.domain.ThemeDownloader
import ru.aokazantsev.devatlas.themes.domain.model.ArchiveLocation
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.DownloadResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.ThemeHelp
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan

@ContributesBinding(AppScope::class)
@Inject
internal class KtorThemeDownloader(
    private val client: HttpClient,
    private val fileSystem: FileSystem,
    private val endpoints: NetworkEndpoints,
) : ThemeDownloader {

    override suspend fun download(plan: ThemeSourcePlan, validators: CacheValidators?, target: Path): DownloadResult =
        try {
            val url = when (val location = plan.location) {
                is ArchiveLocation.Direct -> location.url
                is ArchiveLocation.YandexPublic -> when (val resolved = yandexLink(location)) {
                    is YandexLink.Failed -> return DownloadResult.Failed(resolved.failure)
                    is YandexLink.Found -> resolved.url
                }
            }
            fetch(url, validators, target)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: ArchiveWriteException) {
            fileSystem.delete(target, mustExist = false)
            val reason = error.message ?: "не хватает места или ошибка записи"
            DownloadResult.Failed(ThemeFailure("Не удалось сохранить архив на телефон: $reason"))
        } catch (_: Exception) {
            fileSystem.delete(target, mustExist = false)
            DownloadResult.Failed(ThemeFailure("Не удалось скачать: нет сети или сервер не отвечает. Проверьте интернет и повторите"))
        }

    private suspend fun yandexLink(location: ArchiveLocation.YandexPublic): YandexLink {
        val response = client.get(endpoints.yandexDownloadApiUrl) {
            parameter("public_key", location.publicKey)
            location.path?.let { parameter("path", it) }
        }
        return when {
            response.status == HttpStatusCode.NotFound -> YandexLink.Failed(
                ThemeFailure("Яндекс.Диск не нашёл эту ссылку: проверьте, что папка или файл опубликованы", ThemeHelp.PUBLISH_URL),
            )
            response.status != HttpStatusCode.OK -> YandexLink.Failed(ThemeFailure("Яндекс.Диск ответил ошибкой ${response.status.value}"))
            else -> {
                val href = hrefOf(response.bodyAsText())
                if (href.isNullOrEmpty()) {
                    YandexLink.Failed(ThemeFailure(YANDEX_ODD_ANSWER))
                } else {
                    YandexLink.Found(href)
                }
            }
        }
    }

    private fun hrefOf(body: String): String? = try {
        Json.parseToJsonElement(body).jsonObject["href"]?.jsonPrimitive?.takeIf { it.isString }?.content
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private suspend fun fetch(url: String, validators: CacheValidators?, target: Path): DownloadResult =
        client.prepareGet(url) {
            validators?.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
            validators?.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
        }.execute { response ->
            when {
                response.status == HttpStatusCode.NotModified && validators != null -> DownloadResult.NotModified
                response.status != HttpStatusCode.OK -> DownloadResult.Failed(statusFailure(response.status))
                (response.contentLength() ?: 0L) > MAX_BYTES -> DownloadResult.Failed(tooLarge())
                else -> store(response, target)
            }
        }

    private suspend fun store(response: HttpResponse, target: Path): DownloadResult {
        val sink = writing {
            target.parent?.let(fileSystem::createDirectories)
            fileSystem.sink(target).buffer()
        }
        val total = try {
            copy(response.bodyAsChannel(), sink)
        } finally {
            writing { sink.close() }
        }
        if (total > MAX_BYTES) {
            fileSystem.delete(target, mustExist = false)
            return DownloadResult.Failed(tooLarge())
        }
        return DownloadResult.Downloaded(
            etag = response.headers[HttpHeaders.ETag],
            lastModified = response.headers[HttpHeaders.LastModified],
            sizeBytes = total,
        )
    }

    private suspend fun copy(channel: ByteReadChannel, sink: BufferedSink): Long {
        val buffer = ByteArray(CHUNK_SIZE)
        var total = 0L
        while (total <= MAX_BYTES) {
            val read = channel.readAvailable(buffer, 0, buffer.size)
            if (read == -1) break
            writing { sink.write(buffer, 0, read) }
            total += read
        }
        return total
    }

    private inline fun <T> writing(block: () -> T): T = try {
        block()
    } catch (error: IOException) {
        throw ArchiveWriteException(error)
    }

    private fun statusFailure(status: HttpStatusCode): ThemeFailure = when (status) {
        HttpStatusCode.NotFound -> ThemeFailure("По ссылке ничего нет (ошибка 404). Проверьте адрес", ThemeHelp.PUBLISH_URL)
        HttpStatusCode.Forbidden, HttpStatusCode.Unauthorized ->
            ThemeFailure("Доступ закрыт (ошибка ${status.value}): тема должна быть опубликована для всех", ThemeHelp.PUBLISH_URL)
        else -> ThemeFailure("Сервер ответил ошибкой ${status.value}")
    }

    private fun tooLarge() = ThemeFailure(
        "Архив больше ${ArchiveLimits.Default.maxArchiveBytes / ArchiveLimits.MEGABYTE} МБ",
        ThemeHelp.PAGE_URL + "#archive",
    )

    private sealed interface YandexLink {
        data class Found(val url: String) : YandexLink

        data class Failed(val failure: ThemeFailure) : YandexLink
    }

    private companion object {
        val MAX_BYTES = ArchiveLimits.Default.maxArchiveBytes
        const val CHUNK_SIZE = 8_192
        const val YANDEX_ODD_ANSWER = "Яндекс.Диск вернул непонятный ответ, попробуйте позже"
    }
}
