package ru.aokazantsev.devatlas.e2e

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.onNodeWithText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.themeformat.issue.HelpSection

@RunWith(AndroidJUnit4::class)
class OnboardingE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    @Test
    fun nextIsLockedUntilPolicyIsAccepted() {
        val app = e2e.compose
        app.waitForText("Добро пожаловать")

        app.onNodeWithText("Дальше").assertIsNotEnabled()
        app.clickText("Принимаю политику данных")

        app.waitForToggle("Принимаю политику данных", on = true)
        app.onNodeWithText("Дальше").assertIsEnabled()
        app.clickText("Принимаю политику данных")
        app.waitForToggle("Принимаю политику данных", on = false)
        app.onNodeWithText("Дальше").assertIsNotEnabled()
    }

    @Test
    fun nextButtonStaysAboveNavigationBar() {
        val app = e2e.compose
        app.waitForText("Добро пожаловать")
        val decor = app.activity.window.decorView
        val navigationBarHeight = ViewCompat.getRootWindowInsets(decor)!!.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

        val buttonBottom = app.onNodeWithText("Дальше").fetchSemanticsNode().boundsInWindow.bottom

        assertTrue(buttonBottom <= decor.height - navigationBarHeight)
    }

    @Test
    fun reportsAreOffAndStatisticsAreOnByDefaultAndToggleable() {
        val app = e2e.compose
        app.waitForText("Добро пожаловать")

        app.waitFor(hasText("Отправлять отчёты о сбоях") and isOff())
        app.waitFor(hasText("Отправлять статистику") and isOn())

        app.clickText("Отправлять отчёты о сбоях")
        app.clickText("Отправлять статистику")

        app.waitFor(hasText("Отправлять отчёты о сбоях") and isOn())
        app.waitFor(hasText("Отправлять статистику") and isOff())
    }

    @Test
    fun whatIsSentSheetsExplainBothOptions() {
        val app = e2e.compose
        app.waitForText("Добро пожаловать")

        app.clickText("Что уходит в отчёте о сбое")
        app.waitForTextContaining("фрагмент журнала")
        app.clickText("Закрыть")
        app.waitForGone("Закрыть")

        app.clickText("Что уходит в статистике")
        app.waitForTextContaining("Названия тем, их адреса и тексты не отправляются")
        app.clickText("Закрыть")
    }

    @Test
    fun policyLinkOpensPolicyPage() {
        val app = e2e.compose
        app.waitForText("Добро пожаловать")

        app.clickText("Читать политику")

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(Consent.POLICY_URL)))
    }

    @Test
    fun sampleCardInstallsSampleThemeWithPreview() {
        val app = e2e.compose
        app.acceptPolicy()

        app.clickText("Начать с примера")

        app.waitForText(ThemeZips.SAMPLE_TITLE)
        app.waitForText("Страниц")
        app.waitForText(ThemeZips.SAMPLE_PAGE_COUNT.toString())
        app.waitForText("Размер")
        app.waitForText("Источник")
        app.waitForText(TestNetwork.endpoints().sampleThemeUrl)
        app.clickText("Установить")
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    @Test
    fun linkCardTakesAddressFromClipboard() {
        val app = e2e.compose
        app.acceptPolicy()
        val address = e2e.server.baseUrl + "/files/theme.zip"
        e2e.server.serveZip("/files/theme.zip", ThemeZips.sampleZip())
        copyToClipboard(address)

        app.clickText("Вставить")

        app.waitForText(address)
        app.clickText("Проверить")
        app.installFromPreview(ThemeZips.SAMPLE_TITLE)
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    @Test
    fun pastedMessageWithLinkInstallsTheLinkedTheme() {
        val app = e2e.compose
        app.acceptPolicy()
        val address = e2e.server.baseUrl + "/files/theme.zip"
        e2e.server.serveZip("/files/theme.zip", ThemeZips.sampleZip())
        copyToClipboard("Вот тема по Kotlin: $address.")

        app.clickText("Вставить")
        app.clickText("Проверить")

        app.installFromPreview(ThemeZips.SAMPLE_TITLE)
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    @Test
    fun pasteFromEmptyClipboardSaysSo() {
        val app = e2e.compose
        app.acceptPolicy()
        clearClipboard()

        app.clickText("Вставить")

        app.waitForText("В буфере обмена пусто")
    }

    @Test
    fun checkIsDisabledForBlankAddress() {
        val app = e2e.compose
        app.acceptPolicy()

        app.waitForText("Проверить")
        app.onNodeWithText("Проверить").assertIsNotEnabled()
        app.enterAddress("https://example.com/theme.zip")
        app.waitFor(hasText("Проверить") and androidx.compose.ui.test.isEnabled())
    }

    @Test
    fun howToMakeThemeLinkOpensSitePage() {
        val app = e2e.compose
        app.acceptPolicy()

        app.clickText("Как сделать тему")

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(HelpSection.HELP_PAGE_URL)))
    }

    @Test
    fun cancelOnPreviewReturnsToAddress() {
        val app = e2e.compose
        app.acceptPolicy()
        app.clickText("Начать с примера")
        app.waitForText("Установить")

        app.clickText("Отмена")

        app.waitForText("Добавить по ссылке")
        app.waitForText(TestNetwork.endpoints().sampleThemeUrl)
    }

    private fun copyToClipboard(text: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("theme", text))
        }
    }

    private fun clearClipboard() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(ClipboardManager::class.java)
            clipboard.clearPrimaryClip()
        }
    }
}
