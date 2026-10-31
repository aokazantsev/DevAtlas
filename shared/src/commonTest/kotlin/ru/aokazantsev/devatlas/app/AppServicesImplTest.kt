package ru.aokazantsev.devatlas.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.crash.domain.CrashReports
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppServicesImplTest {

    private val usage = FakeUsageStatistics()
    private var crashSendings = 0
    private var schedules = 0
    private val crashReports = object : CrashReports {
        override fun record(source: String, error: Throwable) = Unit

        override suspend fun sendPending() {
            crashSendings++
        }

        override suspend fun awaitSending() = Unit
    }
    private val scheduler = object : UsageScheduler {
        override fun schedule() {
            schedules++
        }
    }

    private fun TestScope.services() = AppServicesImpl(
        crashReports = crashReports,
        usage = usage,
        scheduler = scheduler,
        journal = FakeJournal(),
        appInfo = AppInfo(versionName = "0.1", isDebug = false),
        platform = FakePlatformBindings(),
        scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
    )

    @Test
    fun rotationDoesNotStartServicesAgain() = runTest {
        val services = services()

        services.start(freshLaunch = true)
        services.start(freshLaunch = false)

        assertEquals(listOf(UsageEvent.APP_START), usage.events)
        assertEquals(1, usage.sent)
        assertEquals(1, crashSendings)
        assertEquals(1, schedules)
    }

    @Test
    fun everyFreshLaunchCountsAndSends() = runTest {
        val services = services()

        services.start(freshLaunch = true)
        services.start(freshLaunch = true)

        assertEquals(2, usage.events.size)
        assertEquals(2, usage.sent)
        assertEquals(2, crashSendings)
    }

    @Test
    fun restoredActivityInNewProcessStartsServices() = runTest {
        val services = services()

        services.start(freshLaunch = false)

        assertEquals(listOf(UsageEvent.APP_START), usage.events)
        assertEquals(1, usage.sent)
        assertEquals(1, crashSendings)
    }
}
