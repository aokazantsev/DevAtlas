package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.DevAtlasApplication
import ru.aokazantsev.devatlas.database.data.entity.BookmarkEntity

@RunWith(AndroidJUnit4::class)
class BookmarksE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private val graph get() = (app.activity.application as DevAtlasApplication).graph

    private fun insertBookmark(kind: String, nodeId: String, title: String, position: Int) = runBlocking {
        graph.bookmarkDao.insert(
            BookmarkEntity(themeId = ThemeZips.SAMPLE_ID, kind = kind, nodeId = nodeId, anchor = 0, title = title, position = position),
        )
    }

    private fun storedTitles() = runBlocking { graph.bookmarkDao.all(ThemeZips.SAMPLE_ID).map { it.title } }

    private fun rowTop(title: String): Float =
        app.onNode(hasText(title) and hasClickAction()).fetchSemanticsNode().boundsInRoot.top

    private fun insertThree() {
        insertBookmark("root", "", "Первая", 0)
        insertBookmark("section", "text", "Вторая", 1)
        insertBookmark("page", "welcome", "Третья", 2)
    }

    private fun storedOrder() = runBlocking { graph.bookmarkDao.all(ThemeZips.SAMPLE_ID).map { it.title to it.position } }

    @Test
    fun pageBookmarkKeepsScrollAndOpensWithTreeBackStack() {
        app.passOnboarding()
        app.openCodePage()
        app.scrollDown()
        app.waitForScrolled()
        val saved = app.scrollValue()

        app.addBookmark("Мой код")
        app.openBookmarks()
        app.waitFor(hasText("Мой код") and hasText("Страница «Код»"))
        app.clickText("Мой код")

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
    fun defaultNameIsScreenTitleAndBookmarkCanBeRenamedAndDeleted() {
        app.passOnboarding()
        app.clickText("Текст")
        app.waitFor(hasText("О разделе") and hasClickAction())
        app.addBookmark()
        app.openBookmarks()
        app.waitFor(hasText("Текст") and hasText("Раздел «Текст»"))

        app.onNodeWithContentDescription("Изменить закладку «Текст»").performClick()
        app.renameBookmark("Тексты")
        app.waitFor(hasText("Тексты") and hasText("Раздел «Текст»"))
        assertEquals(listOf("Тексты"), storedTitles())

        app.onNodeWithContentDescription("Изменить закладку «Тексты»").performClick()
        app.clickText("Удалить закладку")
        app.waitForTextContaining("Закладок пока нет")
        assertEquals(emptyList<String>(), storedTitles())
    }

    @Test
    fun longPressDragMovesBookmarkAndOrderIsKept() {
        app.passOnboarding()
        insertThree()
        app.openBookmarks()
        app.waitForText("Третья")
        val row = app.onNode(hasText("Третья") and hasClickAction())
        val step = row.fetchSemanticsNode().boundsInRoot.height / STEPS_PER_ROW

        row.performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + LONG_PRESS_MARGIN_MILLIS)
            repeat(STEPS_PER_ROW * 2) {
                moveBy(Offset(0f, -step))
                advanceEventTime(FRAME_MILLIS)
            }
            up()
        }

        app.waitUntil(WAIT_MILLIS) { storedTitles() == listOf("Третья", "Первая", "Вторая") }
        assertTrue(rowTop("Третья") < rowTop("Первая"))
        assertTrue(rowTop("Первая") < rowTop("Вторая"))
        app.goBackToContents()
        app.openBookmarks()
        app.waitForText("Третья")
        assertTrue(rowTop("Третья") < rowTop("Первая"))
    }

    @Test
    fun longPressWithoutMoveDoesNotOpenBookmark() {
        app.passOnboarding()
        insertThree()
        app.openBookmarks()
        app.waitForText("Третья")

        app.onNode(hasText("Третья") and hasClickAction()).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + LONG_PRESS_MARGIN_MILLIS)
            up()
        }

        app.waitForIdle()
        app.waitForText("Закладки")
        app.assertNoText("Следующая: О разделе")
        assertEquals(listOf("Первая", "Вторая", "Третья"), storedTitles())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accessibilityActionsMoveBookmark() {
        app.passOnboarding()
        insertThree()
        insertBookmark("page", "gone", "Скрытая", 1)
        app.openBookmarks()
        app.waitForText("Первая")
        app.assertNoText("Скрытая")
        val movable = SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions) and hasClickAction()

        app.onNode(movable and hasText("Первая")).performCustomAccessibilityActionWithLabel("Переместить ниже")

        app.waitUntil(WAIT_MILLIS) { storedTitles() == listOf("Вторая", "Первая", "Третья", "Скрытая") }
        app.waitUntil(WAIT_MILLIS) { rowTop("Вторая") < rowTop("Первая") }
        app.onNode(movable and hasText("Третья")).performCustomAccessibilityActionWithLabel("Переместить выше")
        app.waitUntil(WAIT_MILLIS) { storedTitles() == listOf("Вторая", "Третья", "Первая", "Скрытая") }
        assertEquals(listOf(0, 1, 2, 3), storedOrder().map { it.second })
    }

    @Test
    fun sectionBookmarkOpensSectionWithContentsBelow() {
        app.passOnboarding()
        insertBookmark("section", "text", "Раздел текста", 0)
        app.openBookmarks()

        app.clickText("Раздел текста")

        app.waitFor(hasText("О разделе") and hasClickAction())
        app.pressBack()
        app.waitFor(hasContentDescription("Настройки"))
    }

    @Test
    fun rootBookmarkReopensContents() {
        app.passOnboarding()
        app.addBookmark("Начало")
        app.clickText("Добро пожаловать")
        app.goBackToContents()
        app.openBookmarks()

        app.clickText("Начало")

        app.waitFor(hasContentDescription("Настройки"))
        app.waitForText(ThemeZips.SAMPLE_TITLE)
        app.assertNoText("Начало")
    }

    private companion object {
        const val STEPS_PER_ROW = 4
        const val LONG_PRESS_MARGIN_MILLIS = 200L
        const val FRAME_MILLIS = 16L
        const val WAIT_MILLIS = 10_000L
    }
}
