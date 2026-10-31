package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.DevAtlasApplication
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity
import ru.aokazantsev.devatlas.ui.page.diagram.DIAGRAM_VIEWER_TAG
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ContentsE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private val graph get() = (app.activity.application as DevAtlasApplication).graph

    private fun sampleFile(path: String) = File(app.activity.filesDir, "themes/${ThemeZips.SAMPLE_ID}/$path")

    private fun markViewed(pageId: String) = runBlocking {
        graph.viewedPageDao.upsert(ViewedPageEntity(ThemeZips.SAMPLE_ID, pageId))
    }

    private fun waitForRow(title: String, counter: String? = null, done: Boolean = false) {
        var matcher = hasText(title)
        if (counter != null) matcher = matcher and hasText(counter)
        if (done) matcher = matcher and hasContentDescription("Готово")
        app.waitFor(matcher)
        if (!done) {
            val marked = hasText(title) and hasContentDescription("Готово")
            check(app.onAllNodes(marked).fetchSemanticsNodes().isEmpty()) { "У строки «$title» не должно быть галочки" }
        }
    }

    @Test
    fun treeShowsCountersAndOpensNestedPage() {
        app.passOnboarding()

        app.waitForText("Просмотрено 0 из 12 страниц")
        waitForRow("Текст", counter = "0 из 5")
        waitForRow("Схемы", counter = "0 из 4")
        app.clickText("Текст")
        waitForRow("Код и таблицы", counter = "0 из 2")
        app.clickText("Код и таблицы")
        app.clickText("Код")

        app.waitForTextContaining("fun Greeting(name: String)")
        app.waitForText("Следующая: Таблицы")
    }

    @Test
    fun viewedSwitchUpdatesCountersAndCanBeCleared() {
        app.passOnboarding()
        app.clickText("Добро пожаловать")

        app.clickText("Просмотрено")
        app.waitForToggle("Просмотрено", on = true)
        app.goBackToContents()

        app.waitForText("Просмотрено 1 из 12 страниц")
        waitForRow("Добро пожаловать", done = true)

        app.clickText("Добро пожаловать")
        app.waitForToggle("Просмотрено", on = true)
        app.clickText("Просмотрено")
        app.waitForToggle("Просмотрено", on = false)
        app.goBackToContents()
        app.waitForText("Просмотрено 0 из 12 страниц")
    }

    @Test
    fun sectionIsDoneWhenAllItsPagesAreViewed() {
        app.passOnboarding()

        markViewed("internal-links")
        waitForRow("Ссылки и файлы", counter = "1 из 2")
        markViewed("external-links")

        waitForRow("Ссылки и файлы", counter = "2 из 2", done = true)
        app.waitForText("Просмотрено 2 из 12 страниц")
    }

    @Test
    fun nextOpensFollowingPageInTreeOrderWithoutMarkingAndBackReturnsToSection() {
        app.passOnboarding()
        app.clickText("Текст")
        app.clickText("Списки и цитаты")
        app.waitForText("Следующая: Код")

        app.clickText("Дальше")

        app.waitForTextContaining("fun Greeting(name: String)")
        app.waitForToggle("Просмотрено", on = false)
        app.clickText("Дальше")
        app.waitForText("Следующая: Как устроены схемы")
        app.onNodeWithContentDescription("Назад").performClick()
        waitForRow("Списки и цитаты")
        waitForRow("Код и таблицы", counter = "0 из 2")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun diagramOpensFullscreenAndCloses() {
        app.passOnboarding()
        app.clickText("Схемы")
        app.clickText("Как устроены схемы")
        val diagram = hasContentDescription("Три блока в цветах primary, secondary и tertiary")
        app.waitFor(diagram)

        app.onNode(diagram).performScrollTo().performClick()

        app.waitFor(hasTestTag(DIAGRAM_VIEWER_TAG))
        app.onNodeWithContentDescription("Закрыть").performClick()
        app.waitUntilDoesNotExist(hasTestTag(DIAGRAM_VIEWER_TAG), WAIT_MILLIS)
        app.onNode(diagram).performClick()
        app.waitFor(hasTestTag(DIAGRAM_VIEWER_TAG))
        Espresso.pressBack()
        app.waitUntilDoesNotExist(hasTestTag(DIAGRAM_VIEWER_TAG), WAIT_MILLIS)
        app.waitForText("Одна и та же схема в любой теме приложения.")
    }

    private val viewer = hasTestTag(DIAGRAM_VIEWER_TAG)

    private fun viewerNode(): SemanticsNode = app.onNode(viewer).fetchSemanticsNode()

    private fun viewedDiagramScale(): Float {
        val viewerCenter = viewerNode().boundsInRoot.center
        val diagram = app.onNode(hasContentDescription(DIAGRAM_ALT) and hasAnyAncestor(viewer)).fetchSemanticsNode()
        return (viewerCenter.x - diagram.positionInRoot.x) / (diagram.size.width / 2f)
    }

    private fun rotate(orientation: Int) {
        app.activityRule.scenario.onActivity { it.requestedOrientation = orientation }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun zoomedDiagramSurvivesRotation() {
        app.passOnboarding()
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        try {
            app.waitUntil(WAIT_MILLIS) { app.onRoot().fetchSemanticsNode().size.run { width < height } }
            zoomDiagramAndRotate()
        } finally {
            rotate(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        }
    }

    private fun zoomDiagramAndRotate() {
        app.clickText("Схемы")
        app.clickText("Как устроены схемы")
        val diagram = hasContentDescription(DIAGRAM_ALT)
        app.waitFor(diagram)
        app.onNode(diagram).performScrollTo().performClick()
        app.waitFor(viewer)
        app.waitUntil(WAIT_MILLIS) { runCatching { abs(viewedDiagramScale() - 1f) < SCALE_TOLERANCE }.getOrDefault(false) }

        app.onNode(viewer).performTouchInput {
            pinch(
                start0 = center - Offset(PINCH_START_PX, 0f),
                end0 = center - Offset(PINCH_END_PX, 0f),
                start1 = center + Offset(PINCH_START_PX, 0f),
                end1 = center + Offset(PINCH_END_PX, 0f),
            )
        }
        app.waitForIdle()
        val zoomed = viewedDiagramScale()
        check(zoomed >= MIN_ZOOMED_SCALE) { "Схема не увеличилась щипком: масштаб $zoomed" }
        val width = viewerNode().size.width

        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        app.waitUntil(WAIT_MILLIS) { runCatching { viewerNode().size.width != width }.getOrDefault(false) }
        app.waitForIdle()
        val rotated = viewedDiagramScale()
        check(abs(rotated - zoomed) < SCALE_TOLERANCE) { "После поворота масштаб схемы $rotated вместо $zoomed" }
    }

    @Test
    fun pageLinkOpensPageOnTopAndWebLinkOpensBuiltInPage() {
        app.passOnboarding()
        app.clickText("Ссылки и файлы")
        app.clickText("Ссылки между страницами")

        app.clickLink("в начало")
        app.waitForText("Следующая: О разделе")

        app.onNodeWithContentDescription("Назад").performClick()
        app.clickLink("страницу про сайты и PDF")
        app.clickLink("документация Kotlin Multiplatform")
        app.waitFor(hasContentDescription("Открыть в браузере"))
        app.onNodeWithContentDescription("Открыть в браузере").performClick()
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(KOTLIN_DOCS_URL)))
    }

    @Test
    fun pageFileLostDuringSessionShowsErrorOnPage() {
        app.passOnboarding()
        check(sampleFile("pages/welcome.html").delete())

        app.clickText("Добро пожаловать")

        app.waitForTextContaining("Страница не открывается")
        app.clickText("Назад")
        app.waitForText("Просмотрено 0 из 12 страниц")
    }

    @Test
    fun brokenDiagramShowsPlaceholderAndPageStillWorks() {
        app.passOnboarding()
        sampleFile("diagrams/roles.svg").writeText("это не svg")
        app.clickText("Схемы")

        app.clickText("Как устроены схемы")

        app.waitForText("Схема не открывается")
        app.waitForText("Одна и та же схема в любой теме приложения.")
        app.clickText("Просмотрено")
        app.waitForToggle("Просмотрено", on = true)
    }

    @Test
    fun brokenDiagramWithViewBoxShowsPlaceholderAndPageStillWorks() {
        app.passOnboarding()
        sampleFile("diagrams/roles.svg").writeText(BROKEN_SVG_WITH_VIEWBOX)
        app.clickText("Схемы")

        app.clickText("Как устроены схемы")

        app.waitForText("Схема не открывается")
        app.waitForText("Одна и та же схема в любой теме приложения.")
        app.clickText("Просмотрено")
        app.waitForToggle("Просмотрено", on = true)
    }

    @Test
    fun emptyListsOnPageDoNotCrash() {
        val files = ThemeZips.sampleFiles() + (ThemeZips.CHANGED_PAGE_FILE to EMPTY_LISTS_PAGE.toByteArray())
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.zip(files))
        app.passOnboarding()

        app.clickText("Добро пожаловать")

        app.waitForText("До пустых списков.")
        app.waitForText("После пустых списков.")
        app.clickText("Просмотрено")
        app.waitForToggle("Просмотрено", on = true)
    }

    private companion object {
        const val BROKEN_SVG_WITH_VIEWBOX = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 300\"><rect"
        const val EMPTY_LISTS_PAGE = "<p>До пустых списков.</p><ul></ul><ol></ol><p>После пустых списков.</p>"
        const val WAIT_MILLIS = 10_000L
        const val DIAGRAM_ALT = "Три блока в цветах primary, secondary и tertiary"
        const val PINCH_START_PX = 60f
        const val PINCH_END_PX = 300f
        const val MIN_ZOOMED_SCALE = 2f
        const val SCALE_TOLERANCE = 0.1f
        const val KOTLIN_DOCS_URL = "https://kotlinlang.org/docs/multiplatform.html"
    }
}
