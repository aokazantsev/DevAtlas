package ru.aokazantsev.devatlas.e2e

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

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

    @Test
    fun backFromPageRestoresContentsScroll() {
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.longListZip())
        e2e.launch()
        app.acceptPolicy()
        app.clickText("Начать с примера")
        app.installFromPreview(ThemeZips.LONG_LIST_TITLE)
        app.waitForContentsOf(ThemeZips.LONG_LIST_TITLE)
        app.scrollDown()
        app.waitForScrolled()
        app.waitForIdle()
        val saved = app.scrollValue()
        val label = fullyVisibleRowLabel()

        app.onNode(hasText("Страница $label") and hasClickAction()).performClick()
        app.waitForText("Текст страницы $label")
        app.pressBack()

        app.waitFor(hasText("Страница $label") and hasClickAction())
        app.waitUntil(WAIT_MILLIS) {
            app.onAllNodes(verticalScroll).fetchSemanticsNodes().size == 1 && abs(app.scrollValue() - saved) <= TOLERANCE_PX
        }
        SystemClock.sleep(SETTLE_MILLIS)
        app.waitForIdle()
        val restored = app.scrollValue()
        check(abs(restored - saved) <= TOLERANCE_PX) { "Прокрутка содержания $restored вместо $saved после возврата" }
    }

    private fun fullyVisibleRowLabel(): String {
        val viewport = app.onNode(verticalScroll).fetchSemanticsNode().boundsInRoot
        val visible = (1 until ThemeZips.LONG_LIST_PAGES).map { it.toString().padStart(2, '0') }.filter { label ->
            val row = app.onAllNodes(hasText("Страница $label") and hasClickAction()).fetchSemanticsNodes().firstOrNull()
            row != null && row.boundsInRoot.top >= viewport.top && row.boundsInRoot.bottom <= viewport.bottom
        }
        check(visible.isNotEmpty()) { "На экране нет строки содержания целиком" }
        return visible[visible.size / 2]
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        const val POLL_MILLIS = 100L
        const val SETTLE_MILLIS = 1_000L
        const val TOLERANCE_PX = 3f
        val verticalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    }
}
