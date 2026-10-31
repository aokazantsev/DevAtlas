package ru.aokazantsev.devatlas.update.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import ru.aokazantsev.devatlas.update.domain.model.ReleaseList
import ru.aokazantsev.devatlas.update.domain.model.ReleasesResult
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateCheckerImplTest {

    private var result: ReleasesResult = loaded("0.3", "0.2", "0.1")
    private var loads = 0
    private val releases = object : ReleasesRepository {
        override suspend fun load(): ReleasesResult {
            loads++
            return result
        }
    }
    private val settings = FakeUpdateSettings()
    private val usage = FakeUsageStatistics()
    private val clock = FakeClock(DAY * 10)

    private val checker = AppUpdateCheckerImpl(
        releases = releases,
        settings = settings,
        appInfo = AppInfo(versionName = "0.1-debug", isDebug = true),
        usage = usage,
        journal = FakeJournal(),
        clock = clock,
    )

    @Test
    fun newerReleasesAreChangesFromNewest() = runTest {
        val check = assertIs<UpdateCheck.Available>(checker.check())

        assertEquals("0.3", check.latest.version.toString())
        assertEquals(listOf("0.3", "0.2"), check.changes.map { it.version.toString() })
        assertEquals(false, check.hasOlderChanges)
        assertEquals(listOf(UsageEvent.UPDATE_CHECK), usage.events)
    }

    @Test
    fun installedLatestIsUpToDate() = runTest {
        result = loaded("0.1", "0.0.9")

        assertEquals(UpdateCheck.UpToDate("0.1"), checker.check())
    }

    @Test
    fun fullPageOfNewerReleasesPointsToHistory() = runTest {
        result = ReleasesResult.Loaded(ReleaseList(listOf(release("0.3"), release("0.2")), complete = false))

        assertTrue(assertIs<UpdateCheck.Available>(checker.check()).hasOlderChanges)
    }

    @Test
    fun offlineCheckShowsCachedChangesWithNote() = runTest {
        result = ReleasesResult.Failed("Нет связи с GitHub", cached = ReleaseList(listOf(release("0.2")), complete = true))

        val check = assertIs<UpdateCheck.Available>(checker.check())

        assertEquals("0.2", check.latest.version.toString())
        assertNotNull(check.offlineNote)
    }

    @Test
    fun offlineCheckWithoutCacheFails() = runTest {
        result = ReleasesResult.Failed("Нет связи с GitHub", cached = null)

        assertEquals(UpdateCheck.Failed("Нет связи с GitHub"), checker.check())
    }

    @Test
    fun autoCheckRunsOncePerDayAndAnnouncesVersionOnce() = runTest {
        val found = assertNotNull(checker.checkIfDue())
        assertNull(checker.checkIfDue())
        assertEquals(1, loads)

        checker.markAnnounced(found)
        clock.millis += DAY

        assertNull(checker.checkIfDue())
        assertEquals(2, loads)

        result = loaded("0.4", "0.3", "0.2")
        clock.millis += DAY
        assertEquals("0.4", checker.checkIfDue()?.latest?.version.toString())
    }

    @Test
    fun fullPageThatReachesInstalledVersionHasNoOlderChanges() = runTest {
        result = ReleasesResult.Loaded(ReleaseList(listOf(release("0.3"), release("0.2"), release("0.1")), complete = false))

        assertEquals(false, assertIs<UpdateCheck.Available>(checker.check()).hasOlderChanges)
    }

    @Test
    fun autoCheckIsOffWithSetting() = runTest {
        settings.auto.value = false

        assertNull(checker.checkIfDue())
        assertEquals(0, loads)
    }

    @Test
    fun failedAutoCheckIsRetriedOnNextStart() = runTest {
        result = ReleasesResult.Failed("Нет связи с GitHub", cached = null)
        assertNull(checker.checkIfDue())

        result = loaded("0.2")
        assertNotNull(checker.checkIfDue())
    }

    private fun loaded(vararg versions: String) = ReleasesResult.Loaded(ReleaseList(versions.map(::release), complete = true))

    private fun release(version: String) = AppRelease(AppVersion.parse(version)!!, bodyHtml = "<p>$version</p>", apk = null)

    private class FakeUpdateSettings : UpdateSettingsRepository {
        val auto = MutableStateFlow(true)
        private var last = 0L
        private var announced: String? = null

        override val autoCheck = auto

        override suspend fun setAutoCheck(enabled: Boolean) {
            auto.value = enabled
        }

        override suspend fun lastAutoCheckMillis(): Long = last

        override suspend fun setLastAutoCheckMillis(millis: Long) {
            last = millis
        }

        override suspend fun announcedVersion(): String? = announced

        override suspend fun setAnnouncedVersion(version: String) {
            announced = version
        }
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
