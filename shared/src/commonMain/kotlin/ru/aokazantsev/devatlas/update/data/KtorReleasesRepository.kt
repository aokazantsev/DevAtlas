package ru.aokazantsev.devatlas.update.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import ru.aokazantsev.devatlas.app.AppUserAgent
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.update.domain.ReleasesRepository
import ru.aokazantsev.devatlas.update.domain.model.ReleaseList
import ru.aokazantsev.devatlas.update.domain.model.ReleasesResult

@ContributesBinding(AppScope::class)
@Inject
internal class KtorReleasesRepository(
    private val client: HttpClient,
    private val endpoints: NetworkEndpoints,
    private val appInfo: AppInfo,
    private val fileSystem: FileSystem,
    platform: PlatformBindings,
) : ReleasesRepository {

    private val folder = platform.dataDirectory / UpdateFiles.CACHE_FOLDER
    private val cache = folder / CACHE_FILE

    override suspend fun load(): ReleasesResult = withContext(Dispatchers.IO) {
        val fresh = try {
            val response = client.get(endpoints.releasesUrl) {
                parameter("per_page", ReleasesParser.PAGE_SIZE)
                header(HttpHeaders.Accept, HTML_JSON)
                header(HttpHeaders.UserAgent, AppUserAgent.of(appInfo))
                header(API_VERSION_HEADER, API_VERSION)
            }
            if (response.status == HttpStatusCode.OK) {
                response.bodyAsText()
            } else {
                return@withContext failed("GitHub ответил ошибкой ${response.status.value}")
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return@withContext failed("Нет связи с GitHub: проверьте интернет и повторите")
        }
        val list = ReleasesParser.parse(fresh) ?: return@withContext failed("GitHub прислал непонятный ответ")
        store(fresh)
        ReleasesResult.Loaded(list)
    }

    private fun failed(message: String): ReleasesResult.Failed = ReleasesResult.Failed(message, cached())

    private fun cached(): ReleaseList? = try {
        if (fileSystem.exists(cache)) ReleasesParser.parse(fileSystem.read(cache) { readUtf8() }) else null
    } catch (_: IOException) {
        null
    }

    private fun store(text: String) {
        try {
            fileSystem.createDirectories(folder)
            val temporary = folder / "$CACHE_FILE.tmp"
            fileSystem.write(temporary) { writeUtf8(text) }
            fileSystem.atomicMove(temporary, cache)
        } catch (_: IOException) {
        }
    }

    private companion object {
        const val CACHE_FILE = "releases.json"
        const val HTML_JSON = "application/vnd.github.html+json"
        const val API_VERSION_HEADER = "X-GitHub-Api-Version"
        const val API_VERSION = "2022-11-28"
    }
}
