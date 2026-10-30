package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso

private const val SCROLL_TIMEOUT_MILLIS = 10_000L

private val verticalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

fun ComposeTestRule.scrollValue(): Float =
    onNode(verticalScroll).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

fun ComposeTestRule.scrollDown() {
    waitFor(verticalScroll)
    onNode(verticalScroll).performTouchInput { swipeUp() }
    waitForIdle()
}

fun ComposeTestRule.waitForScrolled() {
    waitUntil(SCROLL_TIMEOUT_MILLIS) { onAllNodes(verticalScroll).fetchSemanticsNodes().size == 1 && scrollValue() > 0f }
}

fun ComposeTestRule.assertRestoredNear(saved: Float) {
    val restored = scrollValue()
    check(restored <= saved) { "Прокрутка $restored ниже сохранённой $saved" }
    val screen = onRoot().fetchSemanticsNode().size.height
    check(saved - restored < screen) { "Прокрутка $restored дальше экрана от сохранённой $saved" }
}

fun ComposeTestRule.addBookmark(name: String? = null) {
    waitForIdle()
    waitFor(hasContentDescription("Добавить закладку"))
    onNodeWithContentDescription("Добавить закладку").performClick()
    waitForText("Новая закладка")
    if (name != null) {
        onNode(hasSetTextAction()).performTextReplacement(name)
        Espresso.closeSoftKeyboard()
    }
    clickText("Сохранить")
    waitForGone("Новая закладка")
}

fun ComposeTestRule.renameBookmark(name: String) {
    waitForText("Удалить закладку")
    onNode(hasSetTextAction()).performTextReplacement(name)
    Espresso.closeSoftKeyboard()
    clickText("Сохранить")
    waitForGone("Удалить закладку")
}

fun ComposeTestRule.openBookmarks() {
    goBackToContents()
    onNodeWithContentDescription("Закладки").performClick()
    waitForText("Закладки")
}

fun ComposeTestRule.openCodePage() {
    clickText("Текст")
    clickText("Код и таблицы")
    clickText("Код")
    waitForTextContaining("fun Greeting(name: String)")
}

fun ComposeTestRule.pressBack() {
    onNodeWithContentDescription("Назад").performClick()
    waitForIdle()
}
