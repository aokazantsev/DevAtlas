package ru.aokazantsev.devatlas.e2e

import androidx.activity.BackEventCompat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        app.onNodeWithText("О приложении").performScrollTo().assertIsDisplayed()

        app.onNodeWithContentDescription("Назад").performClick()
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    @Test
    fun stackScreensChangeWithoutTransition() {
        val app = e2e.compose
        app.passOnboarding()

        app.mainClock.autoAdvance = false
        try {
            app.onNodeWithContentDescription("Настройки").performClick()
            app.assertShownAfterShortTime(shown = "Назад", gone = "Настройки")

            app.onNodeWithContentDescription("Назад").performClick()
            app.assertShownAfterShortTime(shown = "Настройки", gone = "Назад")

            app.onNodeWithContentDescription("Настройки").performClick()
            app.assertShownAfterShortTime(shown = "Назад", gone = "Настройки")

            app.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            app.assertShownAfterShortTime(shown = "Настройки", gone = "Назад")
        } finally {
            app.mainClock.autoAdvance = true
        }
    }

    @Test
    fun predictiveBackGestureDoesNotAnimateScreen() {
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()

        app.mainClock.autoAdvance = false
        try {
            val boundsBefore = app.onNodeWithContentDescription("Назад").getBoundsInRoot()
            app.activityRule.scenario.onActivity { activity ->
                val dispatcher = activity.onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
                dispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, HALF_GESTURE, BackEventCompat.EDGE_LEFT))
            }
            app.mainClock.advanceTimeBy(SHORT_TIME_MILLIS)
            val boundsDuringGesture = app.onNodeWithContentDescription("Назад").getBoundsInRoot()
            check(boundsDuringGesture == boundsBefore) {
                "Экран сдвинулся во время жеста «назад»: $boundsBefore → $boundsDuringGesture"
            }

            app.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            app.assertShownAfterShortTime(shown = "Настройки", gone = "Назад")
        } finally {
            app.mainClock.autoAdvance = true
        }
    }

    @Test
    fun cancelledBackGestureKeepsScreenAndNextBackWorks() {
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()

        app.mainClock.autoAdvance = false
        try {
            val boundsBefore = app.onNodeWithContentDescription("Назад").getBoundsInRoot()
            app.activityRule.scenario.onActivity { activity ->
                val dispatcher = activity.onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
                dispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, HALF_GESTURE, BackEventCompat.EDGE_LEFT))
                dispatcher.dispatchOnBackCancelled()
            }
            app.assertShownAfterShortTime(shown = "Назад", gone = "Настройки")
            val boundsAfterCancel = app.onNodeWithContentDescription("Назад").getBoundsInRoot()
            check(boundsAfterCancel == boundsBefore) {
                "Экран сдвинулся после отмены жеста «назад»: $boundsBefore → $boundsAfterCancel"
            }

            app.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            app.assertShownAfterShortTime(shown = "Настройки", gone = "Назад")
        } finally {
            app.mainClock.autoAdvance = true
        }
    }

    private fun DevAtlasComposeRule.assertShownAfterShortTime(shown: String, gone: String) {
        mainClock.advanceTimeBy(SHORT_TIME_MILLIS)
        check(onAllNodesWithContentDescription(gone).fetchSemanticsNodes().isEmpty()) {
            "«$gone» ещё на экране через $SHORT_TIME_MILLIS мс: переход между экранами анимирован"
        }
        check(onAllNodesWithContentDescription(shown).fetchSemanticsNodes().size == 1) {
            "«$shown» не появилось через $SHORT_TIME_MILLIS мс"
        }
    }

    @Test
    fun aboutShowsVersion() {
        val app = e2e.compose
        app.passOnboarding()
        app.openSettings()

        app.clickText("О приложении")

        app.waitForText("Версия")
    }

    private companion object {
        const val SHORT_TIME_MILLIS = 100L
        const val HALF_GESTURE = 0.5f
    }
}
