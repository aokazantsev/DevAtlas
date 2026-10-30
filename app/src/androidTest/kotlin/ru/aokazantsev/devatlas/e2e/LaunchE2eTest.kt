package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.DevAtlasApplication

@RunWith(AndroidJUnit4::class)
class LaunchE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    @Test
    fun splashPassesToOnboardingOnCleanInstall() {
        e2e.compose.waitForText("Добро пожаловать")

        e2e.compose.onNodeWithText("Дальше").assertIsDisplayed()
        e2e.compose.waitForAppearance(wallpaperColors = true)
    }

    @Test
    fun graphUsesTestServerInsteadOfInternet() {
        val graph = (e2e.compose.activity.application as DevAtlasApplication).graph

        assertEquals(e2e.server.server.url("/pets/devatlas/sample-theme.zip").toString(), graph.endpoints.sampleThemeUrl)
    }
}
