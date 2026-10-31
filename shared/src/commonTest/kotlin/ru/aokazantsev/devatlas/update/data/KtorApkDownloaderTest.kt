package ru.aokazantsev.devatlas.update.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import okio.IOException
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import ru.aokazantsev.devatlas.update.domain.model.ReleaseApk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KtorApkDownloaderTest {

    private val fileSystem = FakeFileSystem()
    private var requests = 0
    private var answer: () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to APK_TEXT }

    private val downloader = KtorApkDownloader(
        client = HttpClient(MockEngine {
            requests++
            val (status, body) = answer()
            respond(body, status)
        }),
        endpoints = NetworkEndpoints.Production,
        appInfo = AppInfo(versionName = "0.1", isDebug = false),
        fileSystem = fileSystem,
        platform = FakePlatformBindings(),
    )

    private fun release(url: String = URL, sha256: String? = APK_TEXT.encodeUtf8().sha256().hex()) =
        AppRelease(AppVersion(listOf(0, 2)), bodyHtml = "", apk = ReleaseApk(url, sizeBytes = 0L, sha256 = sha256))

    @Test
    fun verifiedApkReplacesOldDownloads() = runTest {
        fileSystem.createDirectories(UPDATES)
        fileSystem.write(UPDATES / "DevAtlas-0.1.apk") { writeUtf8("old") }

        val saved = assertIs<ApkDownloadResult.Saved>(downloader.download(release()) { _, _ -> })

        assertEquals(UPDATES / "DevAtlas-0.2.apk", saved.file)
        assertEquals(APK_TEXT, fileSystem.read(saved.file) { readUtf8() })
        assertFalse(fileSystem.exists(UPDATES / "DevAtlas-0.1.apk"))
    }

    @Test
    fun wrongChecksumLeavesNoFile() = runTest {
        val result = downloader.download(release(sha256 = "0".repeat(64))) { _, _ -> }

        assertTrue(assertIs<ApkDownloadResult.Failed>(result).message.contains("не совпал"))
        assertFalse(fileSystem.exists(UPDATES / "DevAtlas-0.2.apk"))
    }

    @Test
    fun releaseWithoutChecksumOrHttpsIsNotDownloaded() = runTest {
        assertIs<ApkDownloadResult.Failed>(downloader.download(release(sha256 = null)) { _, _ -> })
        assertIs<ApkDownloadResult.Failed>(downloader.download(release(url = "http://example.com/a.apk")) { _, _ -> })

        assertEquals(0, requests)
    }

    @Test
    fun networkFailureIsReportedAsNetworkNotDisk() = runTest {
        answer = { throw IOException("connection reset") }

        val failed = assertIs<ApkDownloadResult.Failed>(downloader.download(release()) { _, _ -> })

        assertTrue(failed.message.startsWith("Не удалось скачать обновление"))
    }

    @Test
    fun serverErrorIsNamed() = runTest {
        answer = { HttpStatusCode.NotFound to "" }

        assertEquals(
            ApkDownloadResult.Failed("GitHub ответил ошибкой 404"),
            downloader.download(release()) { _, _ -> },
        )
    }

    private companion object {
        const val URL = "https://github.com/aokazantsev/DevAtlas/releases/download/v0.2/DevAtlas-0.2.apk"
        const val APK_TEXT = "apk bytes"
        val UPDATES = "/data/updates".toPath()
    }
}
