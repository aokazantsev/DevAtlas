package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ru.aokazantsev.devatlas.ui.semantics.APP_ROOT_TAG
import ru.aokazantsev.devatlas.ui.semantics.DarkThemeKey
import ru.aokazantsev.devatlas.ui.semantics.WallpaperColorsKey

private const val SCREEN_TIMEOUT_MILLIS = 10_000L

@OptIn(ExperimentalTestApi::class)
fun DevAtlasComposeRule.waitFor(matcher: SemanticsMatcher) {
    waitUntilAtLeastOneExists(matcher, SCREEN_TIMEOUT_MILLIS)
}

fun DevAtlasComposeRule.waitForText(text: String) {
    waitFor(hasText(text))
}

fun DevAtlasComposeRule.waitForToggle(text: String, on: Boolean) {
    waitFor(hasText(text) and if (on) isOn() else isOff())
}

fun DevAtlasComposeRule.passOnboarding() {
    waitForText("Добро пожаловать")
    onNodeWithText("Дальше").performClick()
    waitForText("Содержание")
}

fun DevAtlasComposeRule.openSettings() {
    onNodeWithContentDescription("Настройки").performClick()
    waitForText("Оформление")
}

fun DevAtlasComposeRule.waitForAppearance(darkTheme: Boolean? = null, wallpaperColors: Boolean? = null) {
    var matcher = hasTestTag(APP_ROOT_TAG)
    if (darkTheme != null) matcher = matcher and SemanticsMatcher.expectValue(DarkThemeKey, darkTheme)
    if (wallpaperColors != null) matcher = matcher and SemanticsMatcher.expectValue(WallpaperColorsKey, wallpaperColors)
    waitFor(matcher)
}
