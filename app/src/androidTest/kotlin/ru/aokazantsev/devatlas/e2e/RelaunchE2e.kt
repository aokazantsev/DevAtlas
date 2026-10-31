package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import ru.aokazantsev.devatlas.DevAtlasApplication
import ru.aokazantsev.devatlas.MainActivity

class RelaunchE2e {
    val server = MockServerRule()
    val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null

    private val scenarioCleanup = object : ExternalResource() {
        override fun after() {
            scenario?.close()
        }
    }

    val chain: RuleChain = RuleChain.outerRule(server).around(ExternalIntentsRule()).around(scenarioCleanup).around(compose)

    val graph
        get() = (ApplicationProvider.getApplicationContext<DevAtlasApplication>()).graph

    val appContext: DevAtlasApplication
        get() = ApplicationProvider.getApplicationContext()

    fun launch() {
        close()
        wakeScreen()
        scenario = ActivityScenario.launch(Intent(appContext, MainActivity::class.java))
    }

    private fun wakeScreen() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf("input keyevent KEYCODE_WAKEUP", "wm dismiss-keyguard").forEach { command ->
            automation.executeShellCommand(command).close()
        }
    }

    fun recreate() {
        scenario?.recreate()
    }

    fun rotate(orientation: Int) {
        wakeScreen()
        scenario?.onActivity { it.requestedOrientation = orientation }
    }

    fun close() {
        scenario?.close()
        scenario = null
    }
}
