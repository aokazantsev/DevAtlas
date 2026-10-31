package ru.aokazantsev.devatlas.e2e

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.crash.data.installCrashCatcher

@RunWith(AndroidJUnit4::class)
class ReportsE2eTest {

    private val e2e = RelaunchE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose
    private val server get() = e2e.server

    private fun waitForBody(path: String): JSONObject {
        app.waitUntil(WAIT_MILLIS) { server.requestBodies(path).isNotEmpty() }
        return JSONObject(server.requestBodies(path).first())
    }

    private fun relaunch() {
        e2e.launch()
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    private fun crashOnBackgroundThread() {
        val installed = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        installCrashCatcher { e2e.graph.crashReports }
        try {
            val crashing = Thread({ throw IllegalStateException("e2e crash") }, CRASHING_THREAD)
            crashing.start()
            crashing.join(WAIT_MILLIS)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(installed)
        }
    }

    private fun switchInSettings(title: String, on: Boolean) {
        app.openSettings()
        app.waitForToggle("Отправлять статистику", on = true)
        app.clickText(title)
        app.waitForToggle(title, on)
    }

    @Test
    fun statisticsGoOnNextStartInServerFormat() {
        e2e.launch()
        app.passOnboarding()

        relaunch()

        val packet = waitForBody(TestNetwork.USAGE_PATH)
        assertTrue(Regex("[0-9a-f]{32}").matches(packet.getString("id")))
        assertEquals("0.1", packet.getString("version"))
        assertTrue(Regex("\\d+\\.\\d+(\\.\\d+)?").matches(packet.getString("windows")))
        val events = packet.getJSONObject("events")
        assertEquals(1, events.getInt("app.start"))
        assertEquals(1, events.getInt("theme.download"))
        val state = packet.getJSONObject("state")
        assertEquals("sample", state.getString("theme"))
        assertEquals("false", state.getString("crash_reports"))
        assertEquals("1", state.getString("themes_count"))
    }

    @Test
    fun statisticsSwitchedOffInSettingsAreNotSent() {
        e2e.launch()
        app.passOnboarding()
        switchInSettings("Отправлять статистику", on = false)

        e2e.launch()
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
        SystemClock.sleep(QUIET_MILLIS)

        assertEquals(0, server.requestCount(TestNetwork.USAGE_PATH))
    }

    @Test
    fun crashReportGoesOnNextStartWhenEnabled() {
        e2e.launch()
        app.passOnboarding()
        switchInSettings("Отправлять отчёты о сбоях", on = true)
        crashOnBackgroundThread()

        relaunch()

        val report = waitForBody(TestNetwork.CRASH_REPORT_PATH)
        assertEquals("DevAtlas", report.getString("app"))
        assertEquals("0.1-debug", report.getString("version"))
        assertTrue(report.getString("os").startsWith("Android "))
        assertEquals("thread $CRASHING_THREAD", report.getString("source"))
        assertTrue(report.getString("error").contains("IllegalStateException: e2e crash"))
        assertTrue(report.getString("log").contains("start 0.1-debug"))
    }

    @Test
    fun crashReportIsDroppedWhenSendingIsOff() {
        e2e.launch()
        app.passOnboarding()
        crashOnBackgroundThread()

        relaunch()
        waitForBody(TestNetwork.USAGE_PATH)

        assertEquals(0, server.requestCount(TestNetwork.CRASH_REPORT_PATH))
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        const val QUIET_MILLIS = 3_000L
        const val CRASHING_THREAD = "e2e-crashing"
    }
}
