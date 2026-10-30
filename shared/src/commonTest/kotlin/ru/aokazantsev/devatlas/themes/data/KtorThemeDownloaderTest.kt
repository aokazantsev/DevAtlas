package ru.aokazantsev.devatlas.themes.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themeformat.archive.ArchiveLimits
import ru.aokazantsev.devatlas.themes.domain.model.ArchiveLocation
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.DownloadResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorThemeDownloaderTest {

    private val fileSystem = FakeFileSystem()
    private val target = "/staging/1/archive.zip".toPath()
    private val requests = mutableListOf<HttpRequestData>()
    private val zipBytes = "PK-zip".encodeToByteArray()

    private fun downloader(
        files: FileSystem = fileSystem,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = KtorThemeDownloader(
        client = HttpClient(MockEngine { request -> requests += request; handler(request) }),
        fileSystem = files,
        endpoints = NetworkEndpoints.Production,
    )

    private fun direct(url: String = "https://example.com/theme.zip") =
        ThemeSourcePlan(address = url, location = ArchiveLocation.Direct(url), subFolder = null)

    private val googleDrive = ThemeSourcePlan(
        address = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view",
        location = ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345"),
        subFolder = null,
    )

    @Test
    fun googleDriveFileIsDownloadedFromUsercontentWithConfirm() = runTest {
        val loader = downloader { respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/zip")) }

        assertIs<DownloadResult.Downloaded>(loader.download(googleDrive, null, target))

        val url = requests.single().url
        assertEquals("drive.usercontent.google.com", url.host)
        assertEquals("1AbCdEfGhIjKlMnOpQrStUvWxYz012345", url.parameters["id"])
        assertEquals("download", url.parameters["export"])
        assertEquals("t", url.parameters["confirm"])
        assertNull(url.parameters["resourcekey"])
    }

    @Test
    fun googleDriveResourceKeyIsSent() = runTest {
        val loader = downloader { respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/zip")) }
        val plan = googleDrive.copy(location = ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345", "0-AbCdEfGhIjKl"))

        assertIs<DownloadResult.Downloaded>(loader.download(plan, null, target))

        assertEquals("0-AbCdEfGhIjKl", requests.single().url.parameters["resourcekey"])
    }

    @Test
    fun googleDrivePageInsteadOfFileSaysOpenAccess() = runTest {
        val loader = downloader {
            respond("<html>Sign in</html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))
        }

        val failure = failure(loader.download(googleDrive, null, target))

        assertTrue(failure.message.contains("Все, у кого есть ссылка"))
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun googleDriveRedirectToSignInSaysOpenAccess() = runTest {
        val loader = downloader { request ->
            if (request.url.host == "drive.usercontent.google.com") {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://accounts.google.com/ServiceLogin"))
            } else {
                respond("<html>Sign in</html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))
            }
        }

        assertTrue(failure(loader.download(googleDrive, null, target)).message.contains("Все, у кого есть ссылка"))
    }

    @Test
    fun googleDriveMalformedContentTypeIsNotHtml() = runTest {
        val loader = downloader { respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "zip;;=")) }

        assertIs<DownloadResult.Downloaded>(loader.download(googleDrive, null, target))
    }

    @Test
    fun googleDriveMissingFileGivesNotFoundText() = runTest {
        val loader = downloader { respond("", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/html")) }

        assertTrue(failure(loader.download(googleDrive, null, target)).message.contains("404"))
    }

    @Test
    fun googleDriveIgnoringValidatorsStillDownloads() = runTest {
        val loader = downloader { respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/octet-stream")) }

        assertIs<DownloadResult.Downloaded>(loader.download(googleDrive, CacheValidators(etag = null, lastModified = "Fri"), target))
    }

    @Test
    fun htmlFromOrdinarySiteIsStillSaved() = runTest {
        val loader = downloader { respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) }

        assertIs<DownloadResult.Downloaded>(loader.download(direct(), null, target))
    }

    private val yandex = ThemeSourcePlan(
        address = "https://disk.yandex.ru/d/key",
        location = ArchiveLocation.YandexPublic("https://disk.yandex.ru/d/key", "/sub"),
        subFolder = null,
    )

    private fun failure(result: DownloadResult) = assertIs<DownloadResult.Failed>(result).failure

    @Test
    fun okResponseIsStoredWithValidators() = runTest {
        val loader = downloader {
            respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ETag to listOf("\"v1\""), HttpHeaders.LastModified to listOf("Mon")))
        }

        val result = assertIs<DownloadResult.Downloaded>(loader.download(direct(), null, target))

        assertEquals("\"v1\"", result.etag)
        assertEquals("Mon", result.lastModified)
        assertEquals(zipBytes.size.toLong(), result.sizeBytes)
        assertTrue(fileSystem.read(target) { readByteArray() }.contentEquals(zipBytes))
    }

    @Test
    fun validatorsAreSentAndNotModifiedIsReported() = runTest {
        val loader = downloader { respond(ByteArray(0), HttpStatusCode.NotModified) }

        val result = loader.download(direct(), CacheValidators(etag = "\"v1\"", lastModified = "Mon"), target)

        assertEquals(DownloadResult.NotModified, result)
        assertEquals("\"v1\"", requests.single().headers[HttpHeaders.IfNoneMatch])
        assertEquals("Mon", requests.single().headers[HttpHeaders.IfModifiedSince])
    }

    @Test
    fun notModifiedWithoutValidatorsIsAnError() = runTest {
        val loader = downloader { respond(ByteArray(0), HttpStatusCode.NotModified) }

        assertTrue(failure(loader.download(direct(), null, target)).message.contains("304"))
    }

    @Test
    fun httpErrorsHaveOwnTexts() = runTest {
        val statuses = listOf(
            HttpStatusCode.NotFound,
            HttpStatusCode.Forbidden,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.InternalServerError,
        )
        val texts = statuses.map { status ->
            failure(downloader { respond(ByteArray(0), status) }.download(direct(), null, target))
        }

        assertTrue(texts[0].message.contains("404"))
        assertTrue(texts[1].message.contains("Доступ закрыт") && texts[1].helpUrl?.endsWith("#publish") == true)
        assertTrue(texts[2].message.contains("Доступ закрыт"))
        assertTrue(texts[3].message.contains("500"))
    }

    @Test
    fun declaredSizeAboveLimitIsRefusedBeforeReading() = runTest {
        val tooBig = (ArchiveLimits.Default.maxArchiveBytes + 1).toString()
        val loader = downloader { respond(zipBytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, tooBig)) }

        val failure = failure(loader.download(direct(), null, target))

        assertTrue(failure.message.contains("50 МБ"))
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun streamAboveLimitWithoutDeclaredSizeIsCutOff() = runTest {
        val body = ByteArray((ArchiveLimits.Default.maxArchiveBytes + 1).toInt())
        val loader = downloader { respond(ByteReadChannel(body), HttpStatusCode.OK) }

        val failure = failure(loader.download(direct(), null, target))

        assertTrue(failure.message.contains("50 МБ"))
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun networkFailureSaysNoNetworkAndLeavesNoFile() = runTest {
        val loader = downloader { throw IOException("connection reset") }

        assertTrue(failure(loader.download(direct(), null, target)).message.contains("нет сети"))
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun diskFailureIsNotReportedAsNetwork() = runTest {
        val failingDisk = object : ForwardingFileSystem(fileSystem) {
            override fun sink(file: Path, mustCreate: Boolean): Sink = throw IOException("No space left on device")
        }
        val loader = downloader(files = failingDisk) { respond(zipBytes, HttpStatusCode.OK) }

        val failure = failure(loader.download(direct(), null, target))

        assertTrue(failure.message.contains("Не удалось сохранить архив"))
        assertTrue(failure.message.contains("No space left"))
    }

    @Test
    fun yandexLinkIsResolvedWithKeyAndPath() = runTest {
        val loader = downloader { request ->
            if (request.url.host == "cloud-api.yandex.net") {
                respond("""{"href":"https://downloader.disk.yandex.ru/zip/abc"}""", HttpStatusCode.OK)
            } else {
                respond(zipBytes, HttpStatusCode.OK)
            }
        }

        assertIs<DownloadResult.Downloaded>(loader.download(yandex, null, target))
        assertEquals("https://disk.yandex.ru/d/key", requests.first().url.parameters["public_key"])
        assertEquals("/sub", requests.first().url.parameters["path"])
        assertEquals("downloader.disk.yandex.ru", requests.last().url.host)
    }

    @Test
    fun yandexAnswersThatAreNotALinkAreExplained() = runTest {
        listOf("not json", "[]", "{}", """{"href":""}""", """{"href":null}""", """{"href":5}""").forEach { body ->
            val loader = downloader { respond(body, HttpStatusCode.OK) }

            val failure = failure(loader.download(yandex, null, target))

            assertTrue(failure.message.contains("Яндекс.Диск"), "$body → ${failure.message}")
        }
    }

    @Test
    fun yandexErrorsAreExplained() = runTest {
        assertTrue(failure(downloader { respond("", HttpStatusCode.NotFound) }.download(yandex, null, target)).message.contains("не нашёл"))
        assertTrue(failure(downloader { respond("", HttpStatusCode.BadGateway) }.download(yandex, null, target)).message.contains("502"))
    }
}
