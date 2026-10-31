package ru.aokazantsev.devatlas.upload.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import okio.IOException
import ru.aokazantsev.devatlas.app.AppUserAgent
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.upload.domain.ServiceUploader
import ru.aokazantsev.devatlas.upload.domain.model.UploadTarget

@ContributesBinding(AppScope::class)
@Inject
internal class KtorServiceUploader(
    private val client: HttpClient,
    private val endpoints: NetworkEndpoints,
    private val appInfo: AppInfo,
    private val journal: AppJournal,
) : ServiceUploader {

    override suspend fun post(target: UploadTarget, json: String): Int? {
        val url = when (target) {
            UploadTarget.CRASH_REPORT -> endpoints.crashReportUrl
            UploadTarget.USAGE -> endpoints.usageUrl
        }
        val status = try {
            client.post(url) {
                header(HttpHeaders.UserAgent, AppUserAgent.of(appInfo))
                setBody(TextContent(json, ContentType.Application.Json))
            }.status.value
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: IOException) {
            journal.write("${target.journalName}: failed: ${error.message}")
            return null
        } catch (_: Exception) {
            journal.write("${target.journalName}: failed")
            return null
        }
        journal.write("${target.journalName}: answer $status")
        return status
    }
}
