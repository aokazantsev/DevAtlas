package ru.aokazantsev.devatlas.usage.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.onboarding.data.OnboardingRepositoryImpl
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.upload.data.KtorServiceUploader
import ru.aokazantsev.devatlas.usage.domain.UsageStateSource
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UsageStatisticsImplTest {

    private val fileSystem = FakeFileSystem()
    private val onboarding = OnboardingRepositoryImpl(InMemoryPreferencesDataStore())
    private val clock = FakeClock(DAY_START)
    private val bodies = mutableListOf<JsonObject>()
    private var status = HttpStatusCode.OK
    private var duringPost: suspend () -> Unit = {}
    private val platform = FakePlatformBindings()
    private val state = object : UsageStateSource {
        override suspend fun state(): Map<String, String> = mapOf("theme_mode" to "dark")
    }

    private fun TestScope.statistics() = UsageStatisticsImpl(
        store = UsageLedgerStoreImpl(fileSystem, platform, clock),
        uploader = KtorServiceUploader(
            client = HttpClient(MockEngine { request ->
                bodies += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                duringPost()
                respond("", status)
            }),
            endpoints = NetworkEndpoints.Production,
            appInfo = AppInfo(versionName = "0.1-debug", isDebug = true),
            journal = FakeJournal(),
        ),
        onboarding = onboarding,
        stateSource = state,
        appInfo = AppInfo(versionName = "0.1-debug", isDebug = true),
        platform = platform,
        scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
    )

    @Test
    fun sendsDayPacketInServerFormatAndOnlyOnce() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()

        statistics.hit(UsageEvent.APP_START)
        statistics.hit(UsageEvent.PDF_OPEN)
        statistics.hit(UsageEvent.PDF_OPEN)
        statistics.send()
        statistics.send()

        assertEquals(1, bodies.size)
        val packet = bodies.single()
        assertTrue(Regex("[0-9a-f]{32}").matches(packet.getValue("id").jsonPrimitive.content))
        assertEquals("2026-10-31", packet.getValue("day").jsonPrimitive.content)
        assertEquals("0.1", packet.getValue("version").jsonPrimitive.content)
        assertEquals("14.0", packet.getValue("windows").jsonPrimitive.content)
        val events = packet.getValue("events").jsonObject
        assertEquals(1, events.getValue("app.start").jsonPrimitive.int)
        assertEquals(2, events.getValue("pdf.open").jsonPrimitive.int)
        assertEquals("dark", packet.getValue("state").jsonObject.getValue("theme_mode").jsonPrimitive.content)
    }

    @Test
    fun keepsInstallIdBetweenPackets() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()

        statistics.hit(UsageEvent.APP_START)
        statistics.send()
        statistics.hit(UsageEvent.THEME_SWITCH)
        statistics.send()

        assertEquals(2, bodies.size)
        assertEquals(bodies[0].getValue("id"), bodies[1].getValue("id"))
    }

    @Test
    fun nothingIsCountedOrSentWithoutConsent() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = false)
        val statistics = statistics()

        statistics.hit(UsageEvent.APP_START)
        statistics.send()

        assertTrue(bodies.isEmpty())
        assertFalse(fileSystem.exists(LEDGER))
    }

    @Test
    fun nothingIsSentBeforePolicyIsAccepted() = runTest {
        val statistics = statistics()

        statistics.hit(UsageEvent.APP_START)
        statistics.send()

        assertTrue(bodies.isEmpty())
    }

    @Test
    fun withdrawnConsentDropsCountedDays() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        statistics.hit(UsageEvent.APP_START)

        onboarding.setUsageStatistics(false)
        statistics.forget()
        onboarding.setUsageStatistics(true)
        statistics.send()

        assertTrue(bodies.isEmpty())
    }

    @Test
    fun dayWithoutUseIsNotSentByBackgroundSending() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        statistics.hit(UsageEvent.APP_START)
        statistics.send()
        clock.millis += DAY

        statistics.send()

        assertEquals(1, bodies.size)
    }

    @Test
    fun serverErrorKeepsDayForNextSending() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        statistics.hit(UsageEvent.APP_START)
        status = HttpStatusCode.ServiceUnavailable

        statistics.send()
        status = HttpStatusCode.OK
        statistics.send()
        statistics.send()

        assertEquals(2, bodies.size)
    }

    @Test
    fun previousDaysGoWithoutStateAndOldDaysAreDropped() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        clock.millis = DAY_START - 8 * DAY
        statistics.hit(UsageEvent.APP_START)
        clock.millis = DAY_START - DAY
        statistics.hit(UsageEvent.THEME_SWITCH)
        clock.millis = DAY_START
        statistics.hit(UsageEvent.APP_START)

        statistics.send()

        assertEquals(listOf("2026-10-30", "2026-10-31"), bodies.map { it.getValue("day").jsonPrimitive.content }.sorted())
        val yesterday = bodies.first { it.getValue("day").jsonPrimitive.content == "2026-10-30" }
        assertEquals(emptyMap(), yesterday.getValue("state").jsonObject.toMap())
    }

    @Test
    fun dayCountedBeforeMovingWestIsKept() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        platform.timeZoneOffset = 3 * HOUR
        clock.millis = DAY_START + 22 * HOUR
        statistics.hit(UsageEvent.APP_START)
        platform.timeZoneOffset = -5 * HOUR

        statistics.send()

        assertEquals(listOf("2026-11-01"), bodies.map { it.getValue("day").jsonPrimitive.content })
    }

    @Test
    fun rejectedPacketIsNotRepeated() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()

        for (rejected in listOf(HttpStatusCode.BadRequest, HttpStatusCode.PayloadTooLarge)) {
            bodies.clear()
            status = rejected
            statistics.hit(UsageEvent.APP_START)
            statistics.send()
            statistics.send()
            assertEquals(1, bodies.size)
        }
    }

    @Test
    fun dayIsLocalDateOfThePhone() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        platform.timeZoneOffset = 3 * HOUR
        clock.millis = DAY_START + 10 * HOUR + 30 * MINUTE
        val statistics = statistics()

        statistics.hit(UsageEvent.APP_START)
        statistics.send()

        assertEquals("2026-11-01", bodies.single().getValue("day").jsonPrimitive.content)
    }

    @Test
    fun consentWithdrawnDuringSendingDoesNotBringCountersBack() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        statistics.hit(UsageEvent.APP_START)
        duringPost = {
            onboarding.setUsageStatistics(false)
            statistics.forget()
        }

        statistics.send()

        assertFalse(fileSystem.exists(LEDGER))
    }

    @Test
    fun eventCountedDuringSendingGoesInNextPacket() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val statistics = statistics()
        statistics.hit(UsageEvent.APP_START)
        duringPost = { statistics.hit(UsageEvent.PDF_OPEN) }

        statistics.send()
        duringPost = {}
        statistics.send()

        assertEquals(2, bodies.size)
        val events = bodies.last().getValue("events").jsonObject
        assertEquals(1, events.getValue("app.start").jsonPrimitive.int)
        assertEquals(1, events.getValue("pdf.open").jsonPrimitive.int)
    }

    private companion object {
        const val HOUR = 60L * 60 * 1000
        const val MINUTE = 60L * 1000
        const val DAY = 24L * 60 * 60 * 1000
        const val DAY_START = 1_793_404_800_000L + 12 * 60 * 60 * 1000
        val LEDGER = "/data/usage/days.json".toPath()
    }
}
