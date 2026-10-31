package ru.aokazantsev.devatlas.e2e

import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ScrollAnchorE2eTest {

    private val e2e = RelaunchE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private val scrollContainer = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun installTallTheme() {
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, TallDiagramTheme.zip())
        e2e.launch()
        app.acceptPolicy()
        app.clickText("Начать с примера")
        app.installFromPreview(TallDiagramTheme.TITLE)
        app.waitForContentsOf(TallDiagramTheme.TITLE)
    }

    private fun openTallPage() {
        installTallTheme()
        app.clickText(TallDiagramTheme.PAGE_TITLE)
        app.waitForTextContaining(TallDiagramTheme.paragraphLabel(1))
    }

    private fun scrollPastDiagrams() {
        app.onNode(hasText(TallDiagramTheme.paragraphLabel(SCROLL_TARGET), substring = true)).performScrollTo()
        app.waitForIdle()
    }

    private fun recordedAnchor(): Int? = runBlocking {
        e2e.graph.readingPointDao.find(TallDiagramTheme.ID)
            ?.takeIf { it.kind == "page" && it.nodeId == TallDiagramTheme.PAGE_ID }
            ?.anchor
    }

    private fun waitForRecordedParagraph(): Int {
        val deadline = SystemClock.uptimeMillis() + WAIT_MILLIS
        while (true) {
            val anchor = recordedAnchor()
            if (anchor != null && anchor >= TallDiagramTheme.DIAGRAMS) return anchor
            check(SystemClock.uptimeMillis() < deadline) { "Точка чтения за схемами не записалась: $anchor" }
            SystemClock.sleep(POLL_MILLIS)
        }
    }

    private fun viewportTop(): Float = app.onNode(scrollContainer).fetchSemanticsNode().positionInRoot.y

    private fun nodeWithText(text: String): SemanticsNode? =
        app.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().firstOrNull()

    private fun paragraphShift(number: Int): Float =
        checkNotNull(nodeWithText(TallDiagramTheme.paragraphLabel(number))).positionInRoot.y - viewportTop()

    private fun scrollBy(pixels: Float) {
        app.onNode(scrollContainer).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, pixels) }
        app.waitForIdle()
    }

    private fun singleScrollContainer(): Boolean = app.onAllNodes(scrollContainer).fetchSemanticsNodes().size == 1

    private fun waitForParagraphAt(number: Int, offset: Float) {
        val label = TallDiagramTheme.paragraphLabel(number)
        app.waitUntil(WAIT_MILLIS) {
            nodeWithText(label) != null && singleScrollContainer() && abs(paragraphShift(number) + offset) <= TOLERANCE_PX
        }
        SystemClock.sleep(SETTLE_MILLIS)
        app.waitForIdle()
        val shift = paragraphShift(number)
        check(abs(shift + offset) <= TOLERANCE_PX) {
            "$label должен начинаться на $offset px выше верха экрана, а смещён на $shift px после загрузки схем"
        }
    }

    private fun waitForScrollValue(expected: Float) {
        app.waitUntil(WAIT_MILLIS) { singleScrollContainer() && abs(app.scrollValue() - expected) <= TOLERANCE_PX }
        SystemClock.sleep(SETTLE_MILLIS)
        app.waitForIdle()
        val restored = app.scrollValue()
        check(abs(restored - expected) <= TOLERANCE_PX) { "Прокрутка $restored вместо $expected после возврата" }
    }

    @Test
    fun continuationBelowDiagramsOpensAnchorBlockAndKeepsPoint() {
        openTallPage()
        scrollPastDiagrams()
        app.waitForScrolled()
        val anchor = waitForRecordedParagraphAfterPause()

        e2e.launch()

        waitForParagraphAt(TallDiagramTheme.paragraphOfBlock(anchor), offset = 0f)
        e2e.close()
        SystemClock.sleep(SETTLE_MILLIS)
        check(recordedAnchor() == anchor) { "Точка чтения сдвинулась после повторного захода: ${recordedAnchor()} вместо $anchor" }
    }

    private fun rootWidth(): Int? = runCatching { app.onRoot().fetchSemanticsNode().size.width }.getOrNull()

    private fun fromPortraitToLandscape(inPortrait: () -> Unit, inLandscape: () -> Unit) {
        e2e.rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        try {
            app.waitUntil(WAIT_MILLIS) {
                runCatching { app.onRoot().fetchSemanticsNode().size.run { width < height } }.getOrDefault(false)
            }
            inPortrait()
            val width = rootWidth()
            e2e.rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            app.waitUntil(WAIT_MILLIS) { rootWidth().let { it != null && it != width } }
            inLandscape()
        } finally {
            e2e.rotate(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        }
    }

    @Test
    fun rotationKeepsBlockAndOffsetInsideItAtTop() {
        openTallPage()
        var offset = 0f
        fromPortraitToLandscape(
            inPortrait = {
                scrollPastDiagrams()
                app.waitForScrolled()
                scrollBy(paragraphShift(SCROLL_TARGET) + OFFSET_PX)
                offset = -paragraphShift(SCROLL_TARGET)
                check(abs(offset - OFFSET_PX) <= TOLERANCE_PX) { "Абзац не встал на $OFFSET_PX px выше верха экрана: $offset" }
            },
            inLandscape = { waitForParagraphAt(SCROLL_TARGET, offset) },
        )
    }

    @Test
    fun rotationWithOffsetBeyondShrunkBlockStopsAtNextBlock() {
        openTallPage()
        var offset = 0f
        fromPortraitToLandscape(
            inPortrait = {
                scrollPastDiagrams()
                app.waitForScrolled()
                val deepOffset = paragraphShift(SCROLL_TARGET + 1) - paragraphShift(SCROLL_TARGET) - NEAR_END_PX
                scrollBy(paragraphShift(SCROLL_TARGET) + deepOffset)
                offset = -paragraphShift(SCROLL_TARGET)
                check(abs(offset - deepOffset) <= TOLERANCE_PX) { "Абзац не встал на $deepOffset px выше верха экрана: $offset" }
            },
            inLandscape = {
                waitForParagraphAt(SCROLL_TARGET + 1, offset = 0f)
                val landscapeHeight = paragraphShift(SCROLL_TARGET + 1) - paragraphShift(SCROLL_TARGET)
                check(landscapeHeight < offset) {
                    "В альбомной абзац $landscapeHeight px не короче смещения $offset px: проверка ничего не проверяет"
                }
            },
        )
    }

    @Test
    fun backFromLinkedPageRestoresOffsetInsideTallBlock() {
        installTallTheme()
        app.clickText(TallDiagramTheme.LONG_PAGE_TITLE)
        app.waitForTextContaining(TallDiagramTheme.LONG_BLOCK_START)
        app.onNode(hasText(TallDiagramTheme.LONG_PAGE_LINK, substring = true)).performScrollTo()
        app.waitForIdle()
        val block = checkNotNull(nodeWithText(TallDiagramTheme.LONG_BLOCK_START))
        val top = viewportTop()
        check(block.positionInRoot.y < top && block.positionInRoot.y + block.size.height > top) {
            "Верх экрана должен быть внутри длинного абзаца"
        }
        val saved = app.scrollValue()

        app.clickLink(TallDiagramTheme.LONG_PAGE_LINK)
        app.waitForTextContaining(TallDiagramTheme.paragraphLabel(1))
        app.pressBack()

        app.waitForTextContaining(TallDiagramTheme.LONG_BLOCK_START)
        waitForScrollValue(saved)
    }

    private fun waitForRecordedParagraphAfterPause(): Int {
        e2e.close()
        return waitForRecordedParagraph()
    }

    private companion object {
        const val SCROLL_TARGET = 15
        const val WAIT_MILLIS = 15_000L
        const val POLL_MILLIS = 100L
        const val SETTLE_MILLIS = 1_000L
        const val TOLERANCE_PX = 3f
        const val OFFSET_PX = 40f
        const val NEAR_END_PX = 10f
    }
}
