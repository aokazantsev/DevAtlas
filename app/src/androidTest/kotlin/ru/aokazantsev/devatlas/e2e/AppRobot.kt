package ru.aokazantsev.devatlas.e2e

import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import ru.aokazantsev.devatlas.ui.semantics.APP_ROOT_TAG
import ru.aokazantsev.devatlas.ui.semantics.DarkThemeKey
import ru.aokazantsev.devatlas.ui.semantics.SurfaceColorKey
import ru.aokazantsev.devatlas.ui.semantics.WallpaperColorsKey

private const val SCREEN_TIMEOUT_MILLIS = 10_000L

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitFor(matcher: SemanticsMatcher) {
    waitUntilAtLeastOneExists(matcher, SCREEN_TIMEOUT_MILLIS)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForGone(text: String) {
    waitUntilDoesNotExist(hasText(text), SCREEN_TIMEOUT_MILLIS)
}

fun ComposeTestRule.waitForText(text: String) {
    waitFor(hasText(text))
}

fun ComposeTestRule.waitForTextContaining(text: String) {
    waitFor(hasText(text, substring = true))
}

fun ComposeTestRule.waitForToggle(text: String, on: Boolean) {
    waitFor(hasText(text) and if (on) isOn() else isOff())
}

fun ComposeTestRule.clickText(text: String) {
    waitForText(text)
    val inScrollable = hasText(text) and hasAnyAncestor(hasScrollAction())
    if (onAllNodes(inScrollable).fetchSemanticsNodes().isNotEmpty()) onNode(inScrollable).performScrollTo()
    onNodeWithText(text).performClick()
}

fun ComposeTestRule.assertNoText(text: String) {
    check(onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) { "Текст «$text» не должен быть на экране" }
}

fun ComposeTestRule.acceptPolicy() {
    waitForText("Добро пожаловать")
    clickText("Принимаю политику данных")
    waitFor(hasText("Дальше") and isEnabled())
    clickText("Дальше")
    waitForText("Добавить по ссылке")
}

fun ComposeTestRule.enterAddress(address: String) {
    waitFor(hasSetTextAction())
    onNode(hasSetTextAction()).performTextReplacement(address)
    Espresso.closeSoftKeyboard()
    waitForIdle()
}

fun ComposeTestRule.checkAddress(address: String) {
    enterAddress(address)
    clickText("Проверить")
}

fun ComposeTestRule.installFromPreview(title: String) {
    waitForText(title)
    waitForText("Установить")
    clickText("Установить")
}

fun ComposeTestRule.installThemeByAddress(address: String, title: String) {
    checkAddress(address)
    installFromPreview(title)
}

fun ComposeTestRule.installSampleFromOnboarding() {
    clickText("Начать с примера")
    installFromPreview(ThemeZips.SAMPLE_TITLE)
    waitForContentsOf(ThemeZips.SAMPLE_TITLE)
}

fun ComposeTestRule.waitForContentsOf(title: String) {
    waitFor(hasContentDescription("Настройки"))
    waitForText(title)
}

fun ComposeTestRule.passOnboarding() {
    acceptPolicy()
    installSampleFromOnboarding()
}

fun ComposeTestRule.openSettings() {
    onNodeWithContentDescription("Настройки").performClick()
    waitForText("Оформление")
}

fun ComposeTestRule.openThemesList() {
    openSettings()
    clickText("Список тем")
    waitForText("Добавить тему")
}

fun ComposeTestRule.openThemeDetails(title: String) {
    openThemesList()
    clickText(title)
    waitForText("Источник")
}

fun ComposeTestRule.openThemeManagement(title: String) {
    openThemeDetails(title)
    clickText("Управлять")
    waitForText("Управление темой")
}

fun ComposeTestRule.goBackTo(text: String) {
    repeat(MAX_BACK_STEPS) {
        if (onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
        onNodeWithContentDescription("Назад").performClick()
        waitForIdle()
    }
    waitForText(text)
}

fun ComposeTestRule.goBackToContents() {
    repeat(MAX_BACK_STEPS) {
        if (onAllNodes(hasContentDescription("Настройки")).fetchSemanticsNodes().isNotEmpty()) return
        onNodeWithContentDescription("Назад").performClick()
        waitForIdle()
    }
    waitFor(hasContentDescription("Настройки"))
}

fun ComposeTestRule.surfaceColor(): Int =
    onNode(hasTestTag(APP_ROOT_TAG)).fetchSemanticsNode().config[SurfaceColorKey]

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.waitForSurfaceColorOtherThan(color: Int) {
    waitUntil(SCREEN_TIMEOUT_MILLIS) { surfaceColor() != color }
}

fun ComposeTestRule.waitForAppearance(darkTheme: Boolean? = null, wallpaperColors: Boolean? = null) {
    var matcher = hasTestTag(APP_ROOT_TAG)
    if (darkTheme != null) matcher = matcher and SemanticsMatcher.expectValue(DarkThemeKey, darkTheme)
    if (wallpaperColors != null) matcher = matcher and SemanticsMatcher.expectValue(WallpaperColorsKey, wallpaperColors)
    waitFor(matcher)
}

fun ActivityScenario<*>.waitForDestroyed() {
    val deadline = SystemClock.uptimeMillis() + SCREEN_TIMEOUT_MILLIS
    while (state != Lifecycle.State.DESTROYED) {
        check(SystemClock.uptimeMillis() < deadline) { "Активити не закрылась: $state" }
        SystemClock.sleep(POLL_MILLIS)
    }
}

private const val MAX_BACK_STEPS = 6
private const val POLL_MILLIS = 50L
