package ru.aokazantsev.devatlas.e2e

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import org.junit.rules.ExternalResource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class MockServerRule : ExternalResource() {

    val server = MockWebServer()

    private val routes = ConcurrentHashMap<String, (RecordedRequest) -> MockResponse>()
    private val log = CopyOnWriteArrayList<String>()
    private val headerLog = CopyOnWriteArrayList<Pair<String, okhttp3.Headers>>()

    fun requestHeaders(path: String): List<okhttp3.Headers> = headerLog.filter { it.first == path }.map { it.second }

    val requestedPaths: List<String>
        get() = log.toList()

    val baseUrl: String
        get() = server.url("/").toString().trimEnd('/')

    fun route(path: String, handler: (RecordedRequest) -> MockResponse) {
        routes[path] = handler
    }

    fun serveZip(path: String, bytes: ByteArray, etag: String? = null, lastModified: String? = null) {
        route(path) { request ->
            val sameEtag = etag != null && request.headers["If-None-Match"] == etag
            val sameDate = lastModified != null && request.headers["If-Modified-Since"] == lastModified
            if (sameEtag || sameDate) {
                MockResponse.Builder().code(NOT_MODIFIED).build()
            } else {
                MockResponse.Builder()
                    .code(OK)
                    .addHeader("Content-Type", "application/zip")
                    .apply { if (etag != null) addHeader("ETag", etag) }
                    .apply { if (lastModified != null) addHeader("Last-Modified", lastModified) }
                    .body(Buffer().write(bytes))
                    .build()
            }
        }
    }

    fun servePdf(path: String, bytes: ByteArray = PDF_BYTES, fileName: String? = null, throttled: Boolean = false) {
        route(path) {
            MockResponse.Builder()
                .code(OK)
                .addHeader("Content-Type", "application/pdf")
                .apply { if (fileName != null) addHeader("Content-Disposition", "attachment; filename=\"$fileName\"") }
                .apply { if (throttled) throttleBody(THROTTLE_BYTES, 1, TimeUnit.SECONDS) }
                .body(Buffer().write(bytes))
                .build()
        }
    }

    fun serveHtml(path: String, html: String) {
        route(path) {
            MockResponse.Builder()
                .code(OK)
                .addHeader("Content-Type", "text/html; charset=utf-8")
                .body(html)
                .build()
        }
    }

    fun requestCount(path: String): Int = log.count { it == path }

    fun serveStatus(path: String, status: Int) {
        route(path) { MockResponse.Builder().code(status).build() }
    }

    fun serveYandexFolder(publicKey: String, archivePath: String, archive: ByteArray, folder: String? = null) {
        serveZip(archivePath, archive)
        route(YANDEX_API_PATH) { request ->
            if (request.url.queryParameter("public_key") == publicKey && request.url.queryParameter("path") == folder) {
                MockResponse.Builder()
                    .code(OK)
                    .addHeader("Content-Type", "application/json")
                    .body("""{"href":"$baseUrl$archivePath"}""")
                    .build()
            } else {
                MockResponse.Builder().code(NOT_FOUND).build()
            }
        }
    }

    fun serveGoogleDriveFile(fileId: String, archive: ByteArray) {
        route(GOOGLE_DRIVE_PATH) { request ->
            val url = request.url
            val asked = url.queryParameter("id") == fileId &&
                url.queryParameter("export") == "download" &&
                url.queryParameter("confirm") == "t"
            if (asked) {
                MockResponse.Builder()
                    .code(OK)
                    .addHeader("Content-Type", "application/zip")
                    .body(Buffer().write(archive))
                    .build()
            } else {
                MockResponse.Builder().code(NOT_FOUND).build()
            }
        }
    }

    fun serveGoogleDriveSignIn() {
        route(GOOGLE_DRIVE_PATH) {
            MockResponse.Builder()
                .code(OK)
                .addHeader("Content-Type", "text/html; charset=utf-8")
                .body("<html><title>Google Drive: Sign-in</title></html>")
                .build()
        }
    }

    override fun before() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                log += path
                headerLog += path to request.headers
                return routes[path]?.invoke(request) ?: MockResponse.Builder().code(NOT_FOUND).build()
            }
        }
        server.start()
        TestNetwork.baseUrl = server.url("/").toString()
        serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.sampleZip())
    }

    override fun after() {
        TestNetwork.baseUrl = null
        server.close()
    }

    companion object {
        const val YANDEX_API_PATH = "/yandex/download"
        const val GOOGLE_DRIVE_PATH = "/gdrive/download"
        const val OK = 200
        const val NOT_MODIFIED = 304
        const val NOT_FOUND = 404
        const val THROTTLE_BYTES = 1024L
        val PDF_BYTES = "%PDF-1.4\n%DevAtlas e2e\n".toByteArray()
    }
}
