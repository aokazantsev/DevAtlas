package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import android.os.SystemClock
import android.webkit.WebView
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinksE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val folder = DownloadsFolderRule()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private fun openLinksPage() {
        WebSite.serve(e2e.server)
        app.passOnboarding()
        app.openLinksPage()
    }

    private fun clickInWebView(id: String) {
        onWebView().withElement(findElement(Locator.ID, id)).perform(webClick())
    }

    private fun tapWebViewUntilRequested(path: String) {
        repeat(TAP_ATTEMPTS) {
            if (e2e.server.requestCount(path) > 0) return
            onView(isAssignableFrom(WebView::class.java)).perform(click())
            runCatching { app.waitUntil(TAP_WAIT_MILLIS) { e2e.server.requestCount(path) > 0 } }
        }
    }

    @Test
    fun webLinkOpensBuiltInPageWithTitleHistoryBrowserAndCopy() {
        openLinksPage()

        app.clickLink("статья на сайте")
        app.waitForText(WebSite.ARTICLE_TITLE)
        tapWebViewUntilRequested(WebSite.NEXT_PATH)
        app.waitForText(WebSite.NEXT_TITLE)
        onView(isAssignableFrom(WebView::class.java)).perform(click())
        app.waitForIntents(allOf(hasAction(Intent.ACTION_VIEW), hasData(WebSite.SITE_MAIL)), 1)
        app.waitForText(WebSite.NEXT_TITLE)
        Espresso.pressBack()
        app.waitForText(WebSite.ARTICLE_TITLE)

        app.clickDescription("Открыть в браузере")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(WebSite.ARTICLE_URL)))
        app.clickDescription("Копировать ссылку")
        app.waitUntil(WAIT_MILLIS) { clipboardText() == WebSite.ARTICLE_URL }

        app.clickDescription("Назад")
        app.waitForTextContaining("Сайт:")
        app.assertNoDescription("Открыть в браузере")
    }

    @Test
    fun pdfLinkDownloadsOpensAndReopensWithoutNetwork() {
        openLinksPage()

        app.clickLink("руководство в PDF")
        app.waitForIntents(pdfOpened, 1)
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 1)
        e2e.server.serveStatus(WebSite.GUIDE_PATH, SERVER_ERROR)

        app.clickLink("руководство в PDF")

        app.waitForIntents(pdfOpened, 2)
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 1) { "Скачанный файл должен открываться без сети" }
        app.openDownloads()
        app.waitForText(WebSite.GUIDE_NAME)
    }

    @Test
    fun longPressCopiesLinkAndRedownloadsUnderSameName() {
        openLinksPage()
        app.longClickLink("руководство в PDF")
        app.clickText("Копировать ссылку")
        app.waitUntil(WAIT_MILLIS) { clipboardText() == WebSite.GUIDE_URL }
        app.clickLink("руководство в PDF")
        app.waitForIntents(pdfOpened, 1)

        app.longClickLink("руководство в PDF")
        app.clickText("Скачать заново")

        app.waitForIntents(pdfOpened, 2)
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 2)
        app.openDownloads()
        app.waitForText(WebSite.GUIDE_NAME)
        app.assertNoText("e2e-guide (1).pdf")
    }

    @Test
    fun linkHasCopyActionForAccessibility() {
        openLinksPage()

        app.performCopyAction("руководство в PDF")

        app.waitUntil(WAIT_MILLIS) { clipboardText() == WebSite.GUIDE_URL }
    }

    @Test
    fun linkMenuSurvivesRotation() {
        openLinksPage()
        app.longClickLink("руководство в PDF")
        app.waitForText("Копировать ссылку")

        app.activityRule.scenario.recreate()

        app.waitForText("Копировать ссылку")
        app.clickText("Копировать ссылку")
        app.waitUntil(WAIT_MILLIS) { clipboardText() == WebSite.GUIDE_URL }
    }

    @Test
    fun downloadSheetIgnoresSwipeDownAndBack() {
        openLinksPage()
        app.clickLink("большой файл")
        app.waitForText("Скачиваю PDF")

        app.onNodeWithText("Скачиваю PDF").performTouchInput { swipeDown() }
        Espresso.pressBackUnconditionally()
        app.waitForIdle()

        app.waitForText("Скачиваю PDF")
        app.clickText("Отмена")
        app.waitForGone("Скачиваю PDF")
    }

    @Test
    fun pdfFoundInWebViewByContentTypeIsDownloaded() {
        openLinksPage()
        app.clickLink("статья на сайте")
        app.waitForText(WebSite.ARTICLE_TITLE)

        clickInWebView("attachment")

        app.waitForIntents(pdfOpened, 1)
        app.waitForText(WebSite.ARTICLE_TITLE)
        app.openDownloads()
        app.waitForText(WebSite.ATTACHMENT_NAME)
    }

    @Test
    fun linkWithoutPdfExtensionOpensWebViewThenDownloadsAndReturns() {
        openLinksPage()

        app.clickLink("отчёт с сайта")

        app.waitForIntents(pdfOpened, 1)
        app.waitForTextContaining("Сайт:")
        app.waitUntil(WAIT_MILLIS) { app.onAllNodes(hasContentDescription("Открыть в браузере")).fetchSemanticsNodes().isEmpty() }
        app.openDownloads()
        app.waitForText(WebSite.REPORT_NAME)
    }

    @Test
    fun downloadCanBeCancelledAndFailureIsExplained() {
        openLinksPage()

        app.clickLink("большой файл")
        app.waitForText("Скачиваю PDF")
        app.clickText("Отмена")
        app.waitForGone("Скачиваю PDF")
        app.clickLink("пропавший файл")
        app.waitForText("PDF не скачался")
        app.waitForText("Сервер ответил ошибкой 404")
        app.clickText("Закрыть")
        app.waitForGone("PDF не скачался")
        app.waitForIntents(pdfOpened, 0)
        app.clickLink("пропавший файл")
        app.waitForText("PDF не скачался")
        e2e.server.servePdf(WebSite.MISSING_PATH)

        app.clickText("Повторить")

        app.waitForIntents(pdfOpened, 1)
        app.openDownloads()
        app.waitForText(WebSite.MISSING_NAME)
        app.assertNoText("e2e-slow.pdf")
    }

    @Test
    fun siteWithoutNetworkShowsErrorAndRetries() {
        openLinksPage()

        app.clickLink("сайт без сети")

        app.waitForTextContaining("Страница не открылась")
        app.clickText("Открыть в браузере")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(OFFLINE_URL)))
        app.clickText("Повторить")
        app.waitForTextContaining("Страница не открылась")
        app.clickDescription("Назад")
        app.waitForTextContaining("Сайт:")
    }

    @Test
    fun siteOpensAfterNetworkReturnsOnRetry() {
        openLinksPage()

        app.clickLink("статья после сбоя")
        app.waitForTextContaining("Страница не открылась")
        e2e.server.startLateSite()
        app.clickText("Повторить")

        app.waitForText(WebSite.ARTICLE_TITLE)
        app.waitForGone("Повторить")
    }

    @Test
    fun pdfLinkWithoutExtensionDownloadsOnRetryAfterNetworkReturns() {
        openLinksPage()

        app.clickLink("отчёт после сбоя")
        app.waitForTextContaining("Страница не открылась")
        e2e.server.startLateSite()
        app.clickText("Повторить")

        app.waitForIntents(pdfOpened, 1)
        app.waitForTextContaining("Сайт:")
        app.waitUntil(WAIT_MILLIS) { app.onAllNodes(hasContentDescription("Открыть в браузере")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun siteCannotOpenOtherAppsWithoutTap() {
        openLinksPage()

        app.clickLink("сайт с автопереходом")
        app.waitForText(WebSite.AUTO_TITLE)
        SystemClock.sleep(AUTO_WAIT_MILLIS)

        app.waitForIntents(allOf(hasAction(Intent.ACTION_VIEW), hasData(WebSite.AUTO_MAIL)), 0)
        app.waitForText(WebSite.AUTO_TITLE)
    }

    @Test
    fun mailAndPhoneLinksGoToSystem() {
        openLinksPage()

        app.clickLink("написать письмо")
        app.clickLink("позвонить")

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("mailto:hello@example.com")))
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("tel:+70000000000")))
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        const val SERVER_ERROR = 500
        const val OFFLINE_URL = "${TestNetwork.OFFLINE_ORIGIN}/page.html"
        const val TAP_ATTEMPTS = 3
        const val AUTO_WAIT_MILLIS = 2_000L
        const val TAP_WAIT_MILLIS = 3_000L
    }
}
