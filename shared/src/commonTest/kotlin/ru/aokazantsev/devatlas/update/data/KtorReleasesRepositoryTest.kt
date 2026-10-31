package ru.aokazantsev.devatlas.update.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.update.domain.model.ReleasesResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class KtorReleasesRepositoryTest {

    private val fileSystem = FakeFileSystem()
    private val requests = mutableListOf<HttpRequestData>()
    private var answer: () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to RELEASES }

    private val repository = KtorReleasesRepository(
        client = HttpClient(MockEngine { request ->
            requests += request
            val (status, body) = answer()
            respond(body, status)
        }),
        endpoints = NetworkEndpoints.Production,
        appInfo = AppInfo(versionName = "0.1", isDebug = false),
        fileSystem = fileSystem,
        platform = FakePlatformBindings(),
    )

    @Test
    fun asksGitHubForHtmlBodiesOfThirtyReleases() = runTest {
        val result = assertIs<ReleasesResult.Loaded>(repository.load())

        assertEquals("0.2", result.list.releases.single().version.toString())
        val request = requests.single()
        assertEquals("30", request.url.parameters["per_page"])
        assertEquals("application/vnd.github.html+json", request.headers[HttpHeaders.Accept])
        assertEquals("2022-11-28", request.headers["X-GitHub-Api-Version"])
    }

    @Test
    fun failureGivesReleasesFromLastSuccessfulAnswer() = runTest {
        repository.load()
        answer = { throw IOException("no network") }

        val failed = assertIs<ReleasesResult.Failed>(repository.load())

        assertEquals("Нет связи с GitHub: проверьте интернет и повторите", failed.message)
        assertEquals("0.2", failed.cached?.releases?.single()?.version.toString())
    }

    @Test
    fun serverErrorAndStrangeAnswerKeepPreviousCache() = runTest {
        repository.load()
        answer = { HttpStatusCode.OK to """{"message": "rate limit"}""" }
        assertIs<ReleasesResult.Failed>(repository.load())
        answer = { HttpStatusCode.InternalServerError to "" }

        val failed = assertIs<ReleasesResult.Failed>(repository.load())

        assertEquals("GitHub ответил ошибкой 500", failed.message)
        assertEquals("0.2", failed.cached?.releases?.single()?.version.toString())
    }

    @Test
    fun failureWithoutCacheHasNothingToShow() = runTest {
        answer = { HttpStatusCode.Forbidden to "" }

        assertNull(assertIs<ReleasesResult.Failed>(repository.load()).cached)
    }

    private companion object {
        const val RELEASES = """[{"tag_name": "v0.2", "body_html": "<p>Новое</p>", "assets": []}]"""
    }
}
