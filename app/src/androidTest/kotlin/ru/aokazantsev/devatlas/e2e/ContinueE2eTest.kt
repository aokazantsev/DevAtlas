package ru.aokazantsev.devatlas.e2e

import android.os.SystemClock
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinueE2eTest {

    private val e2e = RelaunchE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private fun readingPointIs(kind: String, nodeId: String, scrolled: Boolean): Boolean = runBlocking {
        val point = e2e.graph.readingPointDao.find(ThemeZips.SAMPLE_ID) ?: return@runBlocking false
        point.kind == kind && point.nodeId == nodeId && (point.anchor > 0) == scrolled
    }

    private fun relaunchAfterPoint(kind: String, nodeId: String, scrolled: Boolean) {
        e2e.close()
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (!readingPointIs(kind, nodeId, scrolled)) {
            check(SystemClock.uptimeMillis() < deadline) { "Точка чтения не записалась: $kind $nodeId" }
            SystemClock.sleep(POLL_MILLIS)
        }
        e2e.launch()
    }

    @Test
    fun restartOpensLastPageWithScrollAndTreeBackStack() {
        e2e.launch()
        app.passOnboarding()
        app.openCodePage()
        app.scrollDown()
        app.waitForScrolled()
        val saved = app.scrollValue()

        relaunchAfterPoint("page", "code", scrolled = true)

        app.waitForScrolled()
        app.assertRestoredNear(saved)
        app.waitForText("Следующая: Таблицы")
        app.pressBack()
        app.waitFor(hasText("Таблицы") and hasClickAction())
        app.pressBack()
        app.waitFor(hasText("О разделе") and hasClickAction())
        app.pressBack()
        app.waitFor(hasContentDescription("Настройки"))
    }

    @Test
    fun rotationAfterContinuationKeepsSingleTreeBackStack() {
        e2e.launch()
        app.passOnboarding()
        app.openCodePage()
        relaunchAfterPoint("page", "code", scrolled = false)
        app.waitForText("Следующая: Таблицы")

        e2e.recreate()

        app.waitForText("Следующая: Таблицы")
        app.pressBack()
        app.waitFor(hasText("Таблицы") and hasClickAction())
        app.pressBack()
        app.waitFor(hasText("О разделе") and hasClickAction())
        app.pressBack()
        app.waitFor(hasContentDescription("Настройки"))
    }

    @Test
    fun restartReturnsToSectionLeftLast() {
        e2e.launch()
        app.passOnboarding()
        app.openCodePage()
        app.pressBack()
        app.waitFor(hasText("Таблицы") and hasClickAction())

        relaunchAfterPoint("section", "code-and-tables", scrolled = false)

        app.waitFor(hasText("Таблицы") and hasClickAction())
        app.pressBack()
        app.waitFor(hasText("О разделе") and hasClickAction())
    }

    @Test
    fun disabledContinuationStartsFromContents() {
        e2e.launch()
        app.passOnboarding()
        app.openSettings()
        app.waitForToggle("Продолжать с последней точки", on = true)
        app.clickText("Продолжать с последней точки")
        app.waitForToggle("Продолжать с последней точки", on = false)
        app.pressBack()
        app.openCodePage()

        relaunchAfterPoint("page", "code", scrolled = false)

        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
        app.assertNoText("Следующая: Таблицы")
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        const val POLL_MILLIS = 100L
    }
}
