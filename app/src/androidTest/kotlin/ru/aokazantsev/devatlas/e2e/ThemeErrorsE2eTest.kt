package ru.aokazantsev.devatlas.e2e

import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.themeformat.issue.HelpSection

@RunWith(AndroidJUnit4::class)
class ThemeErrorsE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    private val address get() = e2e.server.baseUrl + "/files/theme.zip"

    private fun failWith(bytes: ByteArray) {
        e2e.server.serveZip("/files/theme.zip", bytes)
        val app = e2e.compose
        app.acceptPolicy()
        app.checkAddress(address)
    }

    private fun assertHelpOpens(section: HelpSection) {
        e2e.compose.clickText("Что исправить")
        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(section.url)))
    }

    @Test
    fun fileThatIsNotAZipIsReported() {
        failWith(ThemeZips.notAZip())

        e2e.compose.waitForTextContaining("Архив не читается")
        assertHelpOpens(HelpSection.ARCHIVE)
    }

    @Test
    fun pageWithHtmlWrapperShowsFileAndLinksToHtmlSection() {
        failWith(ThemeZips.withBrokenWelcomePage())

        e2e.compose.waitForTextContaining("pages/welcome.html")
        assertHelpOpens(HelpSection.HTML)
    }

    @Test
    fun archiveWithoutManifestExplainsWhereItShouldBe() {
        failWith(ThemeZips.withoutManifest())

        e2e.compose.waitForTextContaining("Не найден manifest.json")
        assertHelpOpens(HelpSection.ARCHIVE)
    }

    @Test
    fun archiveWithEscapingPathIsRefused() {
        failWith(ThemeZips.withUnsafePath())

        e2e.compose.waitForTextContaining("evil.html")
        assertHelpOpens(HelpSection.ARCHIVE)
    }

    @Test
    fun missingFileGivesHttpErrorText() {
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress(e2e.server.baseUrl + "/files/absent.zip")

        app.waitForTextContaining("ошибка 404")
        app.waitForText("Повторить")
    }

    @Test
    fun serverErrorGivesHttpErrorText() {
        e2e.server.serveStatus("/files/theme.zip", SERVER_ERROR)
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress(address)

        app.waitForTextContaining("ошибкой $SERVER_ERROR")
    }

    @Test
    fun retryAfterFixingTheServerInstallsTheme() {
        val app = e2e.compose
        app.acceptPolicy()
        app.checkAddress(address)
        app.waitForText("Повторить")

        e2e.server.serveZip("/files/theme.zip", ThemeZips.sampleZip())
        app.clickText("Повторить")

        app.installFromPreview(ThemeZips.SAMPLE_TITLE)
        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    @Test
    fun failedAttemptLeavesNothingInstalledAndNoStaging() {
        failWith(ThemeZips.withBrokenWelcomePage())
        e2e.compose.waitForText("Повторить")

        e2e.compose.clickText("Изменить ссылку")

        e2e.compose.waitForText("Добавить по ссылке")
        val data = e2e.compose.activity.filesDir
        assertEquals(false, data.resolve("themes").resolve(ThemeZips.SAMPLE_ID).exists())
        e2e.compose.waitUntil(WAIT_MILLIS) { data.resolve("staging").list().isNullOrEmpty() }
    }

    @Test
    fun warningsDoNotBlockInstallation() {
        val files = ThemeZips.sampleFiles().toMutableMap()
        files["notes.md"] = "заметки".toByteArray()
        e2e.server.serveZip("/files/theme.zip", ThemeZips.zip(files))
        val app = e2e.compose
        app.acceptPolicy()

        app.installThemeByAddress(address, ThemeZips.SAMPLE_TITLE)

        app.waitForContentsOf(ThemeZips.SAMPLE_TITLE)
    }

    private companion object {
        const val SERVER_ERROR = 500
        const val WAIT_MILLIS = 10_000L
    }
}
