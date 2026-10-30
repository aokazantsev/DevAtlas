package ru.aokazantsev.devatlas.e2e

import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    @Test
    fun themeModeSwitchesAndSurvivesRecreate() {
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()
        app.waitForToggle("Система", on = true)

        app.onNodeWithText("Тёмная").performClick()
        app.waitForToggle("Тёмная", on = true)
        app.waitForAppearance(darkTheme = true)

        app.onNodeWithText("Светлая").performClick()
        app.waitForToggle("Светлая", on = true)
        app.waitForToggle("Тёмная", on = false)
        app.waitForAppearance(darkTheme = false)

        app.onNodeWithText("Тёмная").performClick()
        app.waitForAppearance(darkTheme = true)
        app.activityRule.scenario.recreate()
        app.waitForText("Оформление")
        app.waitForAppearance(darkTheme = true)
        app.waitForToggle("Тёмная", on = true)
    }

    @Test
    fun wallpaperColorsToggleIsStored() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()
        app.waitForToggle("Цвета из обоев", on = true)
        val withWallpaper = app.surfaceColor()

        app.onNodeWithText("Цвета из обоев").performClick()

        app.waitForToggle("Цвета из обоев", on = false)
        app.waitForAppearance(wallpaperColors = false)
        app.waitForSurfaceColorOtherThan(withWallpaper)
        val withSeed = app.surfaceColor()
        app.activityRule.scenario.recreate()
        app.waitForText("Оформление")
        app.waitForAppearance(wallpaperColors = false)
        app.waitForToggle("Цвета из обоев", on = false)
        assertEquals(withSeed, app.surfaceColor())

        app.onNodeWithText("Цвета из обоев").performClick()
        app.waitForToggle("Цвета из обоев", on = true)
        app.waitUntil(WAIT_MILLIS) { app.surfaceColor() == withWallpaper }
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
    }

    @Test
    fun wallpaperColorsRowIsHiddenBeforeAndroid12() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()

        app.onAllNodesWithText("Цвета из обоев").assertCountEquals(0)
    }
}
