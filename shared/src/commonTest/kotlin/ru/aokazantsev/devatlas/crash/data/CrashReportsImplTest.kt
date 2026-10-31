package ru.aokazantsev.devatlas.crash.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
import okio.ForwardingFileSystem
import okio.ForwardingSink
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.onboarding.data.OnboardingRepositoryImpl
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.upload.data.KtorServiceUploader
import ru.aokazantsev.devatlas.upload.domain.ServiceUploader
import ru.aokazantsev.devatlas.upload.domain.model.UploadTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrashReportsImplTest {

    private val fileSystem = FakeFileSystem()
    private val onboarding = OnboardingRepositoryImpl(InMemoryPreferencesDataStore())
    private val journal = FakeJournal(tailText = "theme downloaded")
    private val bodies = mutableListOf<JsonObject>()
    private var networkFails = false
    private var answerGate: CompletableDeferred<Unit>? = null

    private val reports = CrashReportsImpl(
        fileSystem = fileSystem,
        platform = FakePlatformBindings(),
        appInfo = AppInfo(versionName = "0.1", isDebug = false),
        clock = FakeClock(),
        journal = journal,
        onboarding = onboarding,
        uploader = KtorServiceUploader(
            client = HttpClient(MockEngine { request ->
                if (networkFails) throw IOException("no network")
                answerGate?.await()
                bodies += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                val known = request.url.toString() == NetworkEndpoints.Production.crashReportUrl
                if (known) respond("") else respondError(HttpStatusCode.NotFound)
            }),
            endpoints = NetworkEndpoints.Production,
            appInfo = AppInfo(versionName = "0.1", isDebug = false),
            journal = journal,
        ),
    )

    @Test
    fun reportGoesOnNextStartWhenEnabled() = runTest {
        onboarding.accept(crashReports = true, usageStatistics = false)

        reports.record("thread main", IllegalStateException("broken page"))
        reports.sendPending()

        val report = bodies.single()
        assertEquals("DevAtlas", report.text("app"))
        assertEquals("0.1", report.text("version"))
        assertEquals("Android 14", report.text("os"))
        assertEquals("Pixel 8", report.text("model"))
        assertEquals("thread main", report.text("source"))
        assertTrue(report.text("error").contains("IllegalStateException: broken page"))
        assertEquals("theme downloaded", report.text("log"))
        assertFalse(fileSystem.exists(PENDING))
        reports.awaitSending()
    }

    @Test
    fun reportIsDroppedWhenSendingIsOff() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)

        reports.record("thread main", IllegalStateException("broken"))
        reports.sendPending()

        assertTrue(bodies.isEmpty())
        assertFalse(fileSystem.exists(PENDING))
    }

    @Test
    fun failedSendingIsNotRepeated() = runTest {
        onboarding.accept(crashReports = true, usageStatistics = false)
        reports.record("thread main", IllegalStateException("broken"))
        networkFails = true

        reports.sendPending()
        networkFails = false
        reports.sendPending()

        assertTrue(bodies.isEmpty())
        assertTrue(journal.lines.any { it.startsWith("crash report: failed") })
    }

    @Test
    fun nothingIsSentWithoutCrash() = runTest {
        onboarding.accept(crashReports = true, usageStatistics = false)

        reports.sendPending()

        assertTrue(bodies.isEmpty())
    }

    @Test
    fun crashDuringSendingWaitsForAnswerAndKeepsNewReport() = runTest {
        onboarding.accept(crashReports = true, usageStatistics = false)
        reports.record("thread main", IllegalStateException("first"))
        val gate = CompletableDeferred<Unit>().also { answerGate = it }

        val sending = launch { reports.sendPending() }
        runCurrent()
        val waiting = async { reports.awaitSending() }
        runCurrent()
        assertFalse(waiting.isCompleted)
        reports.record("thread main", IllegalStateException("second"))
        gate.complete(Unit)
        waiting.await()
        sending.join()

        assertTrue(fileSystem.read(PENDING) { readUtf8() }.contains("second"))
    }

    @Test
    fun crashWhileConsentIsReadStillWaitsForSending() = runTest {
        reports.record("thread main", IllegalStateException("first"))
        val consentGate = CompletableDeferred<Unit>()
        val slowConsent = CrashReportsImpl(
            fileSystem = fileSystem,
            platform = FakePlatformBindings(),
            appInfo = AppInfo(versionName = "0.1", isDebug = false),
            clock = FakeClock(),
            journal = journal,
            onboarding = object : OnboardingRepository by onboarding {
                override val consent = flow {
                    consentGate.await()
                    emitAll(onboarding.consent)
                }
            },
            uploader = object : ServiceUploader {
                override suspend fun post(target: UploadTarget, json: String): Int? = null
            },
        )

        val sending = launch { slowConsent.sendPending() }
        runCurrent()
        val waiting = async { slowConsent.awaitSending() }
        runCurrent()
        assertFalse(waiting.isCompleted)
        consentGate.complete(Unit)
        sending.join()
        waiting.await()
    }

    @Test
    fun secondStartWithoutReportDoesNotEndWaitingForFirstSending() = runTest {
        onboarding.accept(crashReports = true, usageStatistics = false)
        reports.record("thread main", IllegalStateException("first"))
        val gate = CompletableDeferred<Unit>().also { answerGate = it }

        val first = launch { reports.sendPending() }
        runCurrent()
        reports.sendPending()
        val waiting = async { reports.awaitSending() }
        runCurrent()
        assertFalse(waiting.isCompleted)
        gate.complete(Unit)
        first.join()
        waiting.await()
    }

    @Test
    fun interruptedWriteKeepsPreviousReport() {
        reports.record("thread main", IllegalStateException("first"))
        val brokenDisk = object : ForwardingFileSystem(fileSystem) {
            override fun sink(file: Path, mustCreate: Boolean): Sink {
                val real = super.sink(file, mustCreate)
                return object : ForwardingSink(real) {
                    override fun write(source: Buffer, byteCount: Long) {
                        super.write(source, minOf(byteCount, 1L))
                        throw IOException("диск отвалился")
                    }
                }
            }
        }
        val broken = CrashReportsImpl(
            fileSystem = brokenDisk,
            platform = FakePlatformBindings(),
            appInfo = AppInfo(versionName = "0.1", isDebug = false),
            clock = FakeClock(),
            journal = journal,
            onboarding = onboarding,
            uploader = object : ServiceUploader {
                override suspend fun post(target: UploadTarget, json: String): Int? = null
            },
        )

        broken.record("thread main", IllegalStateException("second"))

        assertTrue(fileSystem.read(PENDING) { readUtf8() }.contains("first"))
    }

    private fun JsonObject.text(name: String): String = getValue(name).jsonPrimitive.content

    private companion object {
        val PENDING = "/data/crash/pending.json".toPath()
    }
}
