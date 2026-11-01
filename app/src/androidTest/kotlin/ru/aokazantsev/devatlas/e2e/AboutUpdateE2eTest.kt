package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.BuildConfig
import ru.aokazantsev.devatlas.di.AppLinks

@RunWith(AndroidJUnit4::class)
class AboutUpdateE2eTest {

    private val e2e = RelaunchE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private fun allowInstalls() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("appops set ${e2e.appContext.packageName} REQUEST_INSTALL_PACKAGES allow")
            .let { ParcelFileDescriptor.AutoCloseInputStream(it) }
            .use { it.readBytes() }
    }

    private fun openAbout() {
        app.openSettings()
        app.clickText("О приложении")
        app.waitForText("Версия")
    }

    private fun openUpdate() {
        openAbout()
        app.clickText("Проверить обновление")
    }

    @Test
    fun aboutShowsVersionAndLinks() {
        e2e.server.serveHtml("/privacy", "<html><head><title>Политика e2e</title></head><body>Раздел 8</body></html>")
        e2e.launch()
        app.passOnboarding()
        openAbout()
        app.waitForText(BuildConfig.VERSION_NAME)

        app.clickText("Исходный код на GitHub")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(AppLinks.GITHUB_URL)))

        app.onNodeWithContentDescription("Копировать ссылку: Сайт проекта").performClick()
        app.waitUntil(QUIET_MILLIS) { clipboardText() == AppLinks.SITE_URL }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.assertNoText("Ссылка скопирована")
        } else {
            app.waitForText("Ссылка скопирована")
        }

        app.clickText("Поделиться приложением")
        intended(hasAction(Intent.ACTION_CHOOSER))

        app.clickText("Политика конфиденциальности")
        app.waitForText("Политика e2e")
    }

    @Test
    fun foundUpdateAsksInstallPermissionThenInstallsVerifiedApk() {
        Releases.serve(e2e.server)
        assumeFalse(e2e.appContext.packageManager.canRequestPackageInstalls())
        e2e.launch()
        app.passOnboarding()
        openUpdate()

        app.waitForText("Доступна версия ${Releases.NEW_VERSION}")
        app.waitForTextContaining(Releases.CHANGE_TEXT)
        app.clickText("Обновить")
        app.clickText("Разрешить установку")
        intended(hasAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))

        allowInstalls()
        e2e.recreate()

        app.waitForTextContaining("Подтвердите установку")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasType("application/vnd.android.package-archive")))
    }

    @Test
    fun apkWithWrongChecksumIsNotInstalled() {
        Releases.serve(e2e.server, sha256 = "0".repeat(64))
        e2e.launch()
        app.passOnboarding()
        openUpdate()

        app.clickText("Обновить")

        app.waitForTextContaining("не совпал с опубликованным")
        app.waitForText("Повторить")
    }

    @Test
    fun releaseNotesCodeHasNoCopyButton() {
        Releases.serve(e2e.server, extraNotesHtml = "<pre><code>$NOTES_CODE</code></pre>")
        e2e.launch()
        app.passOnboarding()
        openUpdate()

        app.waitForText("Доступна версия ${Releases.NEW_VERSION}")
        app.waitForTextContaining(NOTES_CODE)

        app.assertNoText("Копировать")
        check(app.onAllNodes(hasContentDescription("Копировать", substring = true)).fetchSemanticsNodes().isEmpty()) {
            "У кода в описании выпуска не должно быть кнопки «Копировать»"
        }
    }

    @Test
    fun releaseWithoutChecksumOffersReleasesPage() {
        Releases.serve(e2e.server, sha256 = null)
        e2e.launch()
        app.passOnboarding()
        openUpdate()
        app.waitForText("Доступна версия ${Releases.NEW_VERSION}")

        app.clickText("Обновить")

        app.waitForText("Открыть страницу выпусков")
        app.assertNoText("Повторить")
        app.clickText("Открыть страницу выпусков")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(AppLinks.RELEASES_PAGE_URL)))
    }

    @Test
    fun latestVersionAndGitHubErrorAreExplained() {
        Releases.serve(e2e.server, versions = listOf(Releases.INSTALLED_VERSION))
        e2e.launch()
        app.passOnboarding()
        openUpdate()
        app.waitForText("Установлена последняя версия ${Releases.INSTALLED_VERSION}")

        e2e.server.serveStatus(TestNetwork.RELEASES_PATH, SERVER_ERROR)
        app.clickText("Проверить ещё раз")

        app.waitForText("GitHub ответил ошибкой $SERVER_ERROR")
        app.waitForText("Повторить")
    }

    @Test
    fun dailyCheckAnnouncesNewVersionOnce() {
        e2e.launch()
        app.passOnboarding()
        Releases.serve(e2e.server)

        e2e.launch()
        app.waitForText("Доступна версия ${Releases.NEW_VERSION}")
        app.clickText("Что нового")
        app.waitForTextContaining(Releases.CHANGE_TEXT)

        e2e.launch()
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
        app.assertNoText("Доступна версия ${Releases.NEW_VERSION}")
    }

    @Test
    fun longHistoryLinksToReleasesPageAndOfflineShowsLastChanges() {
        Releases.serve(e2e.server, versions = (1..HISTORY_PAGE).map { "9.$it" })
        e2e.launch()
        app.passOnboarding()
        openUpdate()

        app.clickText("Более ранние версии — на GitHub")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(AppLinks.RELEASES_PAGE_URL)))

        e2e.server.serveStatus(TestNetwork.RELEASES_PATH, SERVER_ERROR)
        app.pressBack()
        app.clickText("Проверить обновление")
        app.waitForTextContaining("Список изменений — с прошлой проверки")
        app.waitForText("Доступна версия 9.$HISTORY_PAGE")
    }

    @Test
    fun onboardingIsNotCoveredByUpdateNotice() {
        Releases.serve(e2e.server)

        e2e.launch()
        app.waitForText("Добро пожаловать")
        SystemClock.sleep(QUIET_MILLIS)

        app.assertNoText("Доступна версия ${Releases.NEW_VERSION}")
        assertEquals(0, e2e.server.requestCount(TestNetwork.RELEASES_PATH))
    }

    @Test
    fun switchedOffDailyCheckDoesNotAskGitHub() {
        e2e.launch()
        app.passOnboarding()
        app.openSettings()
        app.waitForToggle("Проверять обновления", on = true)
        app.clickText("Проверять обновления")
        app.waitForToggle("Проверять обновления", on = false)
        Releases.serve(e2e.server)

        e2e.launch()
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
        SystemClock.sleep(QUIET_MILLIS)

        app.assertNoText("Доступна версия ${Releases.NEW_VERSION}")
        assertEquals(0, e2e.server.requestCount(TestNetwork.RELEASES_PATH))
    }

    private companion object {
        const val HISTORY_PAGE = 30
        const val QUIET_MILLIS = 3_000L
        const val SERVER_ERROR = 500
        const val NOTES_CODE = "devatlas --version"
    }
}
