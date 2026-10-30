package ru.aokazantsev.devatlas.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity
import java.io.File

@RunWith(AndroidJUnit4::class)
class ThemeStartupE2eTest {

    private val e2e = RelaunchE2e()

    @get:Rule
    val rules = e2e.chain

    private val sampleId = ThemeZips.SAMPLE_ID
    private val sampleTitle = ThemeZips.SAMPLE_TITLE

    private fun themeFolder(id: String) = File(e2e.appContext.filesDir, "themes/$id")

    private fun markViewed(themeId: String, pageId: String) = runBlocking {
        e2e.graph.viewedPageDao.upsert(ViewedPageEntity(themeId, pageId))
    }

    private fun viewedCount(themeId: String) = runBlocking { e2e.graph.viewedPageDao.countForTheme(themeId) }

    private fun installSample() {
        e2e.launch()
        e2e.compose.passOnboarding()
        waitForStagingCleanup()
    }

    private fun waitForStagingCleanup() {
        e2e.compose.waitUntil(WAIT_MILLIS) { File(e2e.appContext.filesDir, "staging").list().isNullOrEmpty() }
    }

    private fun installSecondTheme() {
        e2e.server.serveZip("/themes/other.zip", ThemeZips.withManifestId("other-theme", "Другая тема"))
        val app = e2e.compose
        app.openThemesList()
        app.clickText("Добавить тему")
        app.installThemeByAddress(e2e.server.baseUrl + "/themes/other.zip", "Другая тема")
        app.waitForText("Источник")
        waitForStagingCleanup()
    }

    @Test
    fun acceptedPolicyWithoutThemeReopensOnFirstThemeScreen() {
        e2e.launch()
        e2e.compose.acceptPolicy()

        e2e.launch()

        e2e.compose.waitForText("Добавить по ссылке")
        e2e.compose.assertNoText("Принимаю политику данных")
    }

    @Test
    fun healthyThemeOpensWithoutDamageScreenAfterRestart() {
        installSample()

        e2e.launch()

        e2e.compose.waitForContentsOf(sampleTitle)
        e2e.compose.assertNoText("Скачать заново")
    }

    @Test
    fun missingFilesShowDamagedScreenAndRedownloadRestoresTheme() {
        installSample()
        markViewed(sampleId, "welcome")
        assertTrue(File(themeFolder(sampleId), "manifest.json").delete())

        e2e.launch()

        e2e.compose.waitForTextContaining("Тема «$sampleTitle» повреждена")
        e2e.compose.waitForText("Скачать заново")
        e2e.compose.waitForText("Выбрать другую тему")
        e2e.compose.clickText("Скачать заново")

        e2e.compose.waitForContentsOf(sampleTitle)
        assertTrue(File(themeFolder(sampleId), "manifest.json").exists())
        assertEquals(1, viewedCount(sampleId))
    }

    @Test
    fun missingPageFileIsDetected() {
        installSample()
        assertTrue(File(themeFolder(sampleId), "pages/welcome.html").delete())

        e2e.launch()

        e2e.compose.waitForTextContaining("Пропал файл страницы «pages/welcome.html»")
    }

    @Test
    fun brokenManifestIsDetected() {
        installSample()
        File(themeFolder(sampleId), "manifest.json").writeText("{ не json")

        e2e.launch()

        e2e.compose.waitForTextContaining("manifest.json темы испорчен")
    }

    @Test
    fun failedRedownloadKeepsDamagedScreenWithReason() {
        installSample()
        assertTrue(File(themeFolder(sampleId), "manifest.json").delete())
        e2e.server.serveStatus(TestNetwork.SAMPLE_PATH, NOT_FOUND)
        e2e.launch()
        e2e.compose.waitForText("Скачать заново")

        e2e.compose.clickText("Скачать заново")

        e2e.compose.waitForTextContaining("Не получилось")
        e2e.compose.waitForText("Скачать заново")
        assertEquals(false, File(themeFolder(sampleId), "manifest.json").exists())
    }

    @Test
    fun chooseAnotherThemeForgetsOnlyThemeAndKeepsProgress() {
        installSample()
        markViewed(sampleId, "welcome")
        assertTrue(File(themeFolder(sampleId), "manifest.json").delete())
        e2e.launch()
        e2e.compose.waitForText("Выбрать другую тему")

        e2e.compose.clickText("Выбрать другую тему")

        e2e.compose.waitForText("Добавить по ссылке")
        assertFalse(themeFolder(sampleId).exists())
        assertEquals(1, viewedCount(sampleId))
    }

    @Test
    fun damagedBackgroundThemeIsDroppedAndCurrentOneStaysOpen() {
        installSample()
        installSecondTheme()
        assertTrue(File(themeFolder("other-theme"), "manifest.json").delete())
        e2e.launch()
        e2e.compose.waitForTextContaining("Тема «Другая тема» повреждена")

        e2e.compose.clickText("Выбрать другую тему")

        e2e.compose.waitForContentsOf(sampleTitle)
        e2e.compose.openThemesList()
        e2e.compose.assertNoText("Другая тема")
    }

    @Test
    fun redownloadOfAnotherThemeExplainsWhatToDo() {
        installSample()
        assertTrue(File(themeFolder(sampleId), "manifest.json").delete())
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.withManifestId("intruder", "Чужая тема"))
        e2e.launch()

        e2e.compose.clickText("Скачать заново")

        e2e.compose.waitForTextContaining("Уберите эту тему кнопкой «Выбрать другую тему»")
    }

    private companion object {
        const val NOT_FOUND = 404
        const val WAIT_MILLIS = 10_000L
    }
}
