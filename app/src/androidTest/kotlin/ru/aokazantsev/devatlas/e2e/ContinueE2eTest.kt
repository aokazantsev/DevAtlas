package ru.aokazantsev.devatlas.e2e

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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

    private fun readingPointIs(themeId: String, kind: String, nodeId: String, scrolled: Boolean): Boolean = runBlocking {
        val point = e2e.graph.readingPointDao.find(themeId) ?: return@runBlocking false
        point.kind == kind && point.nodeId == nodeId && (point.anchor > 0) == scrolled
    }

    private fun relaunchAfterPoint(kind: String, nodeId: String, scrolled: Boolean, themeId: String = ThemeZips.SAMPLE_ID) {
        e2e.close()
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (!readingPointIs(themeId, kind, nodeId, scrolled)) {
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

    @Test
    fun restartScrollsParentListToRowWeCameFrom() {
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.longListZip())
        e2e.launch()
        app.acceptPolicy()
        app.clickText("Начать с примера")
        app.installFromPreview(ThemeZips.LONG_LIST_TITLE)
        app.waitForContentsOf(ThemeZips.LONG_LIST_TITLE)
        app.clickText(ThemeZips.LONG_LIST_LAST_PAGE_TITLE)
        app.waitForText(ThemeZips.LONG_LIST_LAST_PAGE_TEXT)

        relaunchAfterPoint("page", ThemeZips.LONG_LIST_LAST_PAGE_ID, scrolled = false, themeId = ThemeZips.LONG_LIST_ID)

        app.waitForText(ThemeZips.LONG_LIST_LAST_PAGE_TEXT)
        app.pressBack()
        app.waitFor(hasText(ThemeZips.LONG_LIST_LAST_PAGE_TITLE) and hasClickAction())
        app.onNode(hasText(ThemeZips.LONG_LIST_LAST_PAGE_TITLE) and hasClickAction()).assertIsDisplayed()
        app.onAllNodes(hasText(ThemeZips.LONG_LIST_FIRST_PAGE_TITLE) and hasClickAction()).fetchSemanticsNodes().let { nodes ->
            if (nodes.isNotEmpty()) app.onNode(hasText(ThemeZips.LONG_LIST_FIRST_PAGE_TITLE) and hasClickAction()).assertIsNotDisplayed()
        }
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        const val POLL_MILLIS = 100L
    }
}
