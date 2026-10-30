package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    @Test
    fun onboardingLeadsToContentsAndSettings() {
        val app = e2e.compose

        app.passOnboarding()
        app.openSettings()
        app.onNodeWithText("О приложении").assertIsDisplayed()

        app.onNodeWithContentDescription("Назад").performClick()
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    @Test
    fun aboutShowsVersion() {
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()

        app.onNodeWithText("О приложении").performClick()

        app.waitForText("Версия")
    }
}
