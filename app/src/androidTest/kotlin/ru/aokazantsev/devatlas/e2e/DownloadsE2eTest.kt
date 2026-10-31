package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class DownloadsE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val folder = DownloadsFolderRule()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private fun downloadGuideAndReport() {
        WebSite.serve(e2e.server)
        app.passOnboarding()
        app.openLinksPage()
        app.clickLink("руководство в PDF")
        app.waitForIntents(pdfOpened, 1)
        app.clickLink("отчёт с сайта")
        app.waitForIntents(pdfOpened, 2)
        app.waitForTextContaining("Сайт:")
        app.openDownloads()
        app.waitForText(WebSite.GUIDE_NAME)
        app.waitForText(WebSite.REPORT_NAME)
    }

    @Test
    fun listSharesDeletesOneFileAndThenAll() {
        downloadGuideAndReport()

        app.clickDescription("Действия с файлом ${WebSite.GUIDE_NAME}")
        app.clickText("Поделиться")
        intended(hasAction(Intent.ACTION_CHOOSER))
        app.clickDescription("Действия с файлом ${WebSite.GUIDE_NAME}")
        app.clickText("Удалить файл")
        app.waitForGone(WebSite.GUIDE_NAME)
        app.waitForText(WebSite.REPORT_NAME)

        app.clickDescription("Удалить все")
        app.waitForText("Удалить все файлы?")
        app.clickText("Удалить все")

        app.waitForTextContaining("Здесь появятся PDF")
    }

    @Test
    fun fileDeletedOutsideAppIsDownloadedAgainOnClick() {
        downloadGuideAndReport()
        shell("rm -f /sdcard/Download/DevAtlas/${WebSite.GUIDE_NAME}")
        app.goBackToContents()
        app.openDownloads()
        app.waitForTextContaining("Файла нет в Загрузках")

        app.clickText(WebSite.GUIDE_NAME)

        app.waitForIntents(pdfOpened, 3)
        check(e2e.server.requestCount(WebSite.GUIDE_PATH) == 2)
    }

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }
}
