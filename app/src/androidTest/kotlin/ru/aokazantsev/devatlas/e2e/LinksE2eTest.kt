package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import android.os.SystemClock
import android.webkit.WebView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.UriMatchers.hasPath
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
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

    private fun tapWebViewUntilRequested(path: String) {
        repeat(TAP_ATTEMPTS) {
            if (e2e.server.requestCount(path) > 0) return
            onView(isAssignableFrom(WebView::class.java)).perform(click())
            runCatching { app.waitUntil(TAP_WAIT_MILLIS) { e2e.server.requestCount(path) > 0 } }
        }
    }

    private fun tapWebViewOnceAndWait(path: String) {
        onView(isAssignableFrom(WebView::class.java)).perform(click())
        app.waitUntil(WAIT_MILLIS) { e2e.server.requestCount(path) > 0 }
    }

    @Test
    fun webLinkOpensBuiltInPageWithTitleHistoryBrowserAndCopy() {
        openLinksPage()

        app.clickLink("статья на сайте")
        app.waitForText(WebSite.ARTICLE_TITLE)
        app.waitForText(WebSite.HOST)
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

    private fun openSecondSitePage() {
        openLinksPage()
        app.clickLink("статья на сайте")
        app.waitForText(WebSite.ARTICLE_TITLE)
        app.waitForText(WebSite.HOST)
        tapWebViewUntilRequested(WebSite.NEXT_PATH)
        app.waitForText(WebSite.NEXT_TITLE)
    }

    private fun waitForHeading(text: String) {
        app.waitUntil(WAIT_MILLIS) {
            runCatching {
                onWebView().withElement(findElement(Locator.TAG_NAME, "h1")).check(webMatches(getText(), containsString(text)))
            }.isSuccess
        }
    }

    @Test
    fun killedRendererShowsRetryAndReopensPage() {
        openSecondSitePage()

        onView(isAssignableFrom(WebView::class.java)).perform(WebViewLoadUrlAction("chrome://kill"))

        app.waitForTextContaining("Страница закрылась")
        app.clickText("Открыть в браузере")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(hasPath(WebSite.NEXT_PATH))))
        app.clickText("Повторить")
        app.waitForGone("Повторить")
        waitForHeading("Вторая")
        app.waitForText(WebSite.NEXT_TITLE)
        app.waitForText(WebSite.servedHost)
    }

    @Test
    fun webPageSurvivesRotationWithHistory() {
        openSecondSitePage()

        app.activityRule.scenario.recreate()

        app.waitForText(WebSite.NEXT_TITLE)
        app.waitForText(WebSite.servedHost)
        waitForHeading("Вторая")
        app.clickDescription("Назад")
        app.waitForText(WebSite.ARTICLE_TITLE)
        app.waitForText(WebSite.HOST)
        waitForHeading("Статья")
    }

    @Test
    fun webViewIsPausedInBackgroundAndResumed() {
        openSitePage(WebSite.VISIBILITY_PATH, WebSite.VISIBILITY_TITLE)
        waitForHeading("start")

        app.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        app.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        app.waitForText(WebSite.VISIBILITY_TITLE)
        waitForHeading("start hidden visible")
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
    fun pdfFoundInWebViewByContentTypeIsDownloadedOnTap() {
        openSitePage(WebSite.ATTACHMENT_TAP_PATH, WebSite.ATTACHMENT_TAP_TITLE)

        tapWebViewUntilRequested(WebSite.ATTACHMENT_PATH)

        app.waitForIntents(pdfOpened, 1)
        app.waitForText(WebSite.ATTACHMENT_TAP_TITLE)
        app.openDownloads()
        app.waitForText(WebSite.ATTACHMENT_NAME)
    }

    @Test
    fun pdfLinkOnSiteIsDownloadedOnTap() {
        openSitePage(WebSite.PDF_TAP_PATH, WebSite.PDF_TAP_TITLE)

        tapWebViewUntilRequested(WebSite.GUIDE_PATH)

        app.waitForIntents(pdfOpened, 1)
        app.waitForText(WebSite.PDF_TAP_TITLE)
    }

    @Test
    fun siteCannotDownloadPdfWithoutTap() {
        openSitePage(WebSite.AUTO_PDF_PATH, WebSite.AUTO_PDF_TITLE)

        app.waitUntil(WAIT_MILLIS) { e2e.server.requestCount(WebSite.ATTACHMENT_PATH) > 0 }
        SystemClock.sleep(AUTO_WAIT_MILLIS)

        app.waitForIntents(pdfOpened, 0)
        app.assertNoText("Скачиваю PDF")
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 0)
        app.waitForText(WebSite.AUTO_PDF_TITLE)
        app.openDownloads()
        app.waitForTextContaining("Здесь появятся PDF")
    }

    @Test
    fun scrollingPageDoesNotLetSiteDownloadPdf() {
        openSitePage(WebSite.SCROLL_PDF_PATH, WebSite.SCROLL_PDF_TITLE)

        repeat(SCROLL_SWIPES) { index ->
            val swipe = if (index % 2 == 0) ViewActions.swipeUp() else ViewActions.swipeDown()
            onView(isAssignableFrom(WebView::class.java)).perform(swipe)
        }

        app.waitUntil(WAIT_MILLIS) { e2e.server.requestCount(WebSite.ATTACHMENT_PATH) > 0 }
        SystemClock.sleep(AUTO_WAIT_MILLIS)
        app.waitForIntents(pdfOpened, 0)
        app.assertNoText("Скачиваю PDF")
        app.waitForText(WebSite.SCROLL_PDF_TITLE)
    }

    @Test
    fun oneTapGivesOneDownloadEvenWhenSiteAddsAnother() {
        openSitePage(WebSite.PDF_THEN_SCRIPT_PATH, WebSite.PDF_THEN_SCRIPT_TITLE)

        tapWebViewOnceAndWait(WebSite.GUIDE_PATH)

        app.waitForIntents(pdfOpened, 1)
        app.waitUntil(WAIT_MILLIS) { e2e.server.requestCount(WebSite.ATTACHMENT_PATH) > 0 }
        SystemClock.sleep(AUTO_WAIT_MILLIS)
        app.waitForIntents(pdfOpened, 1)
        app.openDownloads()
        app.waitForText(WebSite.GUIDE_FILE_NAME)
        app.assertNoText(WebSite.ATTACHMENT_NAME)
    }

    @Test
    fun pdfBehindRedirectIsDownloadedOnTap() {
        openSitePage(WebSite.PDF_REDIRECT_TAP_PATH, WebSite.PDF_REDIRECT_TAP_TITLE)

        tapWebViewUntilRequested(WebSite.PDF_REDIRECT_PATH)

        app.waitForIntents(pdfOpened, 1)
        SystemClock.sleep(AUTO_WAIT_MILLIS)
        app.waitForIntents(pdfOpened, 1)
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 1) { "PDF должен скачаться ровно один раз" }
        app.waitForText(WebSite.PDF_REDIRECT_TAP_TITLE)
        app.openDownloads()
        app.waitForText(WebSite.GUIDE_FILE_NAME)
    }

    @Test
    fun attachmentBehindRedirectIsDownloadedOnTap() {
        openSitePage(WebSite.ATTACHMENT_REDIRECT_TAP_PATH, WebSite.ATTACHMENT_REDIRECT_TAP_TITLE)

        tapWebViewUntilRequested(WebSite.ATTACHMENT_REDIRECT_PATH)

        app.waitForIntents(pdfOpened, 1)
        SystemClock.sleep(AUTO_WAIT_MILLIS)
        app.waitForIntents(pdfOpened, 1)
        app.waitForText(WebSite.ATTACHMENT_REDIRECT_TAP_TITLE)
        app.openDownloads()
        app.waitForText(WebSite.ATTACHMENT_NAME)
        app.assertNoText("attachment-name (1).pdf")
    }

    @Test
    fun tapToOrdinaryPageDoesNotLetItDownloadPdf() {
        openSitePage(WebSite.ORDINARY_TAP_PATH, WebSite.ORDINARY_TAP_TITLE)

        tapWebViewOnceAndWait(WebSite.AUTO_PDF_PATH)

        app.waitForText(WebSite.AUTO_PDF_TITLE)
        app.waitUntil(WAIT_MILLIS) { e2e.server.requestCount(WebSite.ATTACHMENT_PATH) > 0 }
        SystemClock.sleep(AUTO_WAIT_MILLIS)
        app.waitForIntents(pdfOpened, 0)
        app.assertNoText("Скачиваю PDF")
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 0)
        app.openDownloads()
        app.waitForTextContaining("Здесь появятся PDF")
    }

    @Test
    fun pdfFromSlowServerIsDownloadedAfterTap() {
        openSitePage(WebSite.SLOW_TAP_PATH, WebSite.SLOW_TAP_TITLE)

        tapWebViewUntilRequested(WebSite.SLOW_ATTACHMENT_PATH)

        app.waitUntil(SLOW_WAIT_MILLIS) { Intents.getIntents().count(pdfOpened::matches) >= 1 }
        app.waitForIntents(pdfOpened, 1)
        app.openDownloads()
        app.waitForText(WebSite.SLOW_ATTACHMENT_NAME)
    }

    @Test
    fun downloadLinkWorksFromKeyboard() {
        openSitePage(WebSite.DOWNLOAD_LINK_PATH, WebSite.DOWNLOAD_LINK_TITLE)

        pressEnterInWebView()

        app.waitForIntents(pdfOpened, 1)
        app.openDownloads()
        app.waitForText(WebSite.ATTACHMENT_NAME)
    }

    @Test
    fun longHostKeepsItsEndVisible() {
        openLinksPage()

        app.clickLink("сайт с длинным адресом")
        app.waitForText(WebSite.ARTICLE_TITLE)
        app.waitForText(TestNetwork.LONG_HOST)

        assertLongHostEndVisible()
    }

    @Test
    fun longHostWithoutTitleKeepsItsEndVisible() {
        openLinksPage()

        app.clickLink("страница без заголовка")
        waitForHeading(WebSite.UNTITLED_HEADING)
        app.waitForIdle()
        app.waitForText(TestNetwork.LONG_HOST)

        assertLongHostEndVisible()
    }

    private fun assertLongHostEndVisible() {
        val layouts = mutableListOf<TextLayoutResult>()
        val hostNode = app.onNodeWithText(TestNetwork.LONG_HOST).fetchSemanticsNode()
        hostNode.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        check(layout.isLineEllipsized(0)) { "Хост должен не помещаться в шапку, иначе проверка ничего не проверяет" }
        check(layout.getLineEnd(0, visibleEnd = true) == TestNetwork.LONG_HOST.length) { "Конец домена должен быть виден" }
    }

    private fun openSitePage(path: String, title: String) {
        openLinksPage()
        app.clickLink("статья на сайте")
        app.waitForText(WebSite.ARTICLE_TITLE)
        onView(isAssignableFrom(WebView::class.java)).perform(WebViewLoadUrlAction(WebSite.servedUrl(path)))
        app.waitForText(title)
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
        const val SCROLL_SWIPES = 4
        const val SLOW_WAIT_MILLIS = 30_000L
    }
}
