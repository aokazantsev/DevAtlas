package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.DevAtlasApplication
import ru.aokazantsev.devatlas.database.data.entity.BookmarkEntity
import ru.aokazantsev.devatlas.database.data.entity.ReadingPointEntity
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity
import java.io.File

@RunWith(AndroidJUnit4::class)
class ThemeManagementE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose
    private val sampleTitle = ThemeZips.SAMPLE_TITLE
    private val otherTitle = "Другая тема"
    private val otherPath = "/themes/other.zip"
    private val otherAddress get() = e2e.server.baseUrl + otherPath

    private val graph get() = (app.activity.application as DevAtlasApplication).graph

    private fun markViewed(themeId: String, pageId: String) = runBlocking {
        graph.viewedPageDao.upsert(ViewedPageEntity(themeId, pageId))
    }

    private fun viewedCount(themeId: String) = runBlocking { graph.viewedPageDao.countForTheme(themeId) }

    private fun addBookmark(pageId: String, title: String, kind: String = "page") = runBlocking {
        graph.bookmarkDao.insert(
            BookmarkEntity(themeId = ThemeZips.SAMPLE_ID, kind = kind, nodeId = pageId, anchor = 0, title = title, position = 0),
        )
    }

    private fun bookmarkTitles() = runBlocking { graph.bookmarkDao.all(ThemeZips.SAMPLE_ID).map { it.title } }

    private fun setReadingPoint(pageId: String) = runBlocking {
        graph.readingPointDao.upsert(ReadingPointEntity(ThemeZips.SAMPLE_ID, kind = "page", nodeId = pageId, anchor = 1))
    }

    private fun readingPoint() = runBlocking { graph.readingPointDao.find(ThemeZips.SAMPLE_ID) }

    private fun themeFolder(id: String) = File(app.activity.filesDir, "themes/$id")

    private fun installOtherTheme() = installTheme(otherPath, "other-theme", otherTitle)

    private fun installTheme(path: String, id: String, title: String) {
        e2e.server.serveZip(path, ThemeZips.withManifestId(id, title))
        app.openThemesList()
        app.clickText("Добавить тему")
        app.installThemeByAddress(e2e.server.baseUrl + path, title)
        app.waitForText("Источник")
    }

    private fun applyOtherTheme() = applyTheme(otherTitle)

    private fun applyTheme(title: String) {
        app.clickText("Применить")
        app.waitForContentsOf(title)
    }

    @Test
    fun addThemeFromSettingsHidesSampleCardAndOpensDetails() {
        app.passOnboarding()
        app.openThemesList()
        app.clickText("Добавить тему")
        app.waitForText("Ссылка на тему")
        app.assertNoText("Начать с примера")
        e2e.server.serveZip(otherPath, ThemeZips.withManifestId("other-theme", otherTitle))

        app.installThemeByAddress(otherAddress, otherTitle)

        app.waitForText("Источник")
        app.waitForText(otherAddress)
        app.waitForText("Применить")
        app.assertNoText("Это текущая тема")
    }

    @Test
    fun secondThemeDoesNotStealCurrentAndListMarksCurrent() {
        app.passOnboarding()
        installOtherTheme()
        app.goBackTo("Добавить тему")

        app.waitForText("Текущая · 12 страниц")
        app.waitForText(otherTitle)
    }

    @Test
    fun applyOpensThemeAndResetsBackStack() {
        app.passOnboarding()
        installOtherTheme()

        applyOtherTheme()
        Espresso.pressBackUnconditionally()

        app.activityRule.scenario.waitForDestroyed()
    }

    @Test
    fun appliedThemeBecomesCurrentInList() {
        app.passOnboarding()
        installOtherTheme()
        applyOtherTheme()

        app.openThemesList()

        app.waitForText("Текущая · 12 страниц")
        app.waitForText("12 страниц")
    }

    @Test
    fun sameIdAsksBeforeReplacingAndCancelKeepsOldSource() {
        app.passOnboarding()
        e2e.server.serveZip("/other/sample.zip", ThemeZips.sampleZip())
        app.openThemesList()
        app.clickText("Добавить тему")

        app.checkAddress(e2e.server.baseUrl + "/other/sample.zip")
        app.waitForTextContaining("Эта тема уже установлена")
        app.clickText("Установить")
        app.waitForText(
            "Заменить источник и содержимое темы «$sampleTitle»? " +
                "Отметки «Просмотрено» и закладки сохранятся у страниц и разделов, которые остались в теме.",
        )
        app.clickText("Не заменять")
        app.waitForGone("Не заменять")
        app.clickText("Отмена")

        app.goBackTo("Текущая · 12 страниц")
        app.clickText(sampleTitle)
        app.waitForText("Источник")
        app.waitForText(TestNetwork.endpoints().sampleThemeUrl)
    }

    @Test
    fun replaceDialogStaysUsableWithLargeFont() {
        app.passOnboarding()
        e2e.server.serveZip("/other/sample.zip", ThemeZips.sampleZip())

        app.withFontScale(LARGE_FONT_SCALE) {
            app.openThemesList()
            app.clickText("Добавить тему")
            app.checkAddress(e2e.server.baseUrl + "/other/sample.zip")
            app.waitForTextContaining("Эта тема уже установлена")
            app.clickText("Установить")

            app.waitFor(hasText(REPLACE_QUESTION) and (hasScrollAction() or hasAnyAncestor(hasScrollAction())))
            listOf("Не заменять", "Заменить").forEach { label ->
                app.onNodeWithText(label).assertIsDisplayed().assertHasClickAction()
                assertLabelFits(label)
            }
            app.clickText("Заменить")

            app.waitForText("Источник")
            app.waitForText(e2e.server.baseUrl + "/other/sample.zip")
        }
    }

    private fun assertLabelFits(label: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        app.onNode(hasText(label), useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertFalse("Подпись «$label» не должна обрезаться", layout.hasVisualOverflow)
        assertFalse("Подпись «$label» не должна заканчиваться многоточием", (0 until layout.lineCount).any(layout::isLineEllipsized))
    }

    @Test
    fun sameIdReplaceUpdatesSourceKeepsProgress() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        e2e.server.serveZip("/other/sample.zip", ThemeZips.sampleZip())
        app.openThemesList()
        app.clickText("Добавить тему")

        app.checkAddress(e2e.server.baseUrl + "/other/sample.zip")
        app.waitForText("Установить")
        app.clickText("Установить")
        app.clickText("Заменить")

        app.waitForText("Источник")
        app.waitForText(e2e.server.baseUrl + "/other/sample.zip")
        assertEquals(1, viewedCount(ThemeZips.SAMPLE_ID))
    }

    @Test
    fun updateWithoutChangesSaysSoByHashWhenServerHasNoEtag() {
        app.passOnboarding()
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForText("Изменений нет")
        app.clickText("Закрыть")
        app.waitForText("Обновить")
    }

    @Test
    fun updateSendsEtagAndGets304() {
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.sampleZip(), etag = "\"v1\"")
        app.passOnboarding()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.changedSampleZip(), etag = "\"v1\"")
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForText("Изменений нет")
        assertEquals("\"v1\"", e2e.server.requestHeaders(TestNetwork.SAMPLE_PATH).last()["If-None-Match"])
    }

    @Test
    fun updateSendsLastModifiedAndGets304() {
        val date = "Wed, 21 Oct 2026 07:28:00 GMT"
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.sampleZip(), lastModified = date)
        app.passOnboarding()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.changedSampleZip(), lastModified = date)
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForText("Изменений нет")
        assertEquals(date, e2e.server.requestHeaders(TestNetwork.SAMPLE_PATH).last()["If-Modified-Since"])
    }

    @Test
    fun repackedSameContentIsNoChange() {
        app.passOnboarding()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.zip(ThemeZips.sampleFiles().entries.reversed().associate { it.toPair() }))
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForText("Изменений нет")
    }

    @Test
    fun updateKeepsReadingPointOfSurvivingPage() {
        app.passOnboarding()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.changedSampleZip())
        app.openThemeDetails(sampleTitle)
        setReadingPoint("welcome")

        app.clickText("Обновить")
        app.waitForTextContaining("Страниц добавлено: 1, удалено: 1, изменено: 1")
        app.clickText("Обновить")

        app.waitForText("Тема обновлена")
        assertEquals("welcome", readingPoint()?.nodeId)
    }

    @Test
    fun changedDiagramOnlyIsOfferedWithItsOwnText() {
        app.passOnboarding()
        val files = ThemeZips.sampleFiles().toMutableMap()
        val diagram = String(files.getValue("diagrams/tree.svg"))
        files["diagrams/tree.svg"] = diagram.replace("</svg>", "<rect width=\"1\" height=\"1\"/></svg>").toByteArray()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.zip(files))
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForTextContaining("Страницы не изменились: обновились схемы или другие файлы темы.")
    }

    @Test
    fun updateShowsSummaryAndAppliesChangesKeepingProgressOfSurvivingPages() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        markViewed(ThemeZips.SAMPLE_ID, ThemeZips.REMOVED_PAGE_ID)
        addBookmark("welcome", "Осталась")
        addBookmark(ThemeZips.REMOVED_PAGE_ID, "Исчезла")
        addBookmark("code-and-tables", "Вложенный раздел", kind = "section")
        addBookmark("", "Корень", kind = "root")
        addBookmark("gone-section", "Исчезнувший раздел", kind = "section")
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.changedSampleZip())
        app.openThemeDetails(sampleTitle)
        setReadingPoint(ThemeZips.REMOVED_PAGE_ID)

        app.clickText("Обновить")

        app.waitForTextContaining("Страниц добавлено: 1, удалено: 1, изменено: 1")
        app.waitForTextContaining("Отметки «Просмотрено» и закладки удалённых страниц и разделов сбросятся")
        app.clickText("Обновить")

        app.waitForText("Тема обновлена")
        assertEquals(1, viewedCount(ThemeZips.SAMPLE_ID))
        assertEquals(listOf("Осталась", "Вложенный раздел", "Корень"), bookmarkTitles())
        assertEquals(null, readingPoint())
        assertTrue(File(themeFolder(ThemeZips.SAMPLE_ID), "pages/extra.html").exists())
        assertFalse(File(themeFolder(ThemeZips.SAMPLE_ID), "pages/links/internal.html").exists())
        app.clickText("Закрыть")
        app.goBackToContents()
        app.waitForText("Новая страница")
    }

    @Test
    fun updateCancelLeavesOldContent() {
        app.passOnboarding()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.changedSampleZip())
        app.openThemeDetails(sampleTitle)
        app.clickText("Обновить")
        app.waitForText("Есть обновление")

        app.clickText("Отмена")

        app.waitForText("Обновить")
        assertFalse(File(themeFolder(ThemeZips.SAMPLE_ID), "pages/extra.html").exists())
        app.waitUntil(WAIT_MILLIS) { File(app.activity.filesDir, "staging").list().isNullOrEmpty() }
    }

    @Test
    fun updateRefusesAnotherThemeAtTheSameLink() {
        app.passOnboarding()
        e2e.server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.withManifestId("intruder", "Чужая тема"))
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForTextContaining("По этой ссылке другая тема: «Чужая тема»")
        app.waitForTextContaining("подмена другой темой запрещена")
        app.clickText("Закрыть")
        assertTrue(File(themeFolder(ThemeZips.SAMPLE_ID), "manifest.json").exists())
        assertFalse(themeFolder("intruder").exists())
    }

    @Test
    fun updateFailureShowsReason() {
        app.passOnboarding()
        e2e.server.serveStatus(TestNetwork.SAMPLE_PATH, SERVER_ERROR)
        app.openThemeDetails(sampleTitle)

        app.clickText("Обновить")

        app.waitForText("Не удалось обновить")
        app.waitForTextContaining("ошибкой $SERVER_ERROR")
        app.clickText("Закрыть")
    }

    @Test
    fun changingSourceChecksSameIdAndKeepsContent() {
        app.passOnboarding()
        e2e.server.serveZip("/moved/sample.zip", ThemeZips.sampleZip())
        app.openThemeManagement(sampleTitle)
        app.onNodeWithText("Проверить и сохранить ссылку").assertIsNotEnabled()

        app.enterAddress(e2e.server.baseUrl + "/moved/sample.zip")
        app.onNodeWithText("Проверить и сохранить ссылку").assertIsEnabled()
        app.clickText("Проверить и сохранить ссылку")

        app.waitForTextContaining("Ссылка источника будет заменена. Содержимое не изменилось.")
        app.clickText("Обновить")
        app.waitForText("Источник обновлён")
        app.clickText("Закрыть")
        app.goBackTo("Источник")
        app.waitForText(e2e.server.baseUrl + "/moved/sample.zip")
    }

    @Test
    fun changingSourceWithChangedContentUpdatesBoth() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        e2e.server.serveZip("/moved/sample.zip", ThemeZips.changedSampleZip())
        app.openThemeManagement(sampleTitle)

        app.enterAddress(e2e.server.baseUrl + "/moved/sample.zip")
        app.clickText("Проверить и сохранить ссылку")

        app.waitForTextContaining("Ссылка источника будет заменена. Страниц добавлено: 1, удалено: 1, изменено: 1.")
        app.clickText("Обновить")
        app.waitForText("Источник и содержимое обновлены")
        assertTrue(File(themeFolder(ThemeZips.SAMPLE_ID), "pages/extra.html").exists())
        assertEquals(1, viewedCount(ThemeZips.SAMPLE_ID))
    }

    @Test
    fun changingSourceToAnotherThemeIsRefused() {
        app.passOnboarding()
        e2e.server.serveZip("/moved/other.zip", ThemeZips.withManifestId("intruder", "Чужая тема"))
        app.openThemeManagement(sampleTitle)

        app.enterAddress(e2e.server.baseUrl + "/moved/other.zip")
        app.clickText("Проверить и сохранить ссылку")

        app.waitForTextContaining("По этой ссылке другая тема")
        app.clickText("Закрыть")
        app.goBackTo("Источник")
        app.waitForText(TestNetwork.endpoints().sampleThemeUrl)
    }

    @Test
    fun changingSourceWithBadAddressShowsRejection() {
        app.passOnboarding()
        app.openThemeManagement(sampleTitle)

        app.enterAddress("https://drive.google.com/drive/folders/1FVTUAyi_CQqIaymG9SXMAcMY5yW-Y6L0")
        app.clickText("Проверить и сохранить ссылку")

        app.waitForTextContaining("Папку Google Диска скачать нельзя")
        app.clickText("Закрыть")
    }

    @Test
    fun continueIsLockedUntilAnOptionIsChosen() {
        app.passOnboarding()
        app.openThemeManagement(sampleTitle)

        app.onNodeWithText("Продолжить").assertIsNotEnabled()
        app.clickText("Сбросить прогресс")
        app.waitFor(hasText("Сбросить прогресс") and isOn())
        app.onNodeWithText("Продолжить").assertIsEnabled()
        app.clickText("Сбросить прогресс")
        app.waitFor(hasText("Сбросить прогресс") and isOff())
        app.onNodeWithText("Продолжить").assertIsNotEnabled()
    }

    @Test
    fun resetProgressOnlyKeepsThemeAndStaysOnScreen() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        markViewed(ThemeZips.SAMPLE_ID, "headings")
        addBookmark("welcome", "Закладка")
        setReadingPoint("headings")
        app.openThemeManagement(sampleTitle)

        app.clickText("Сбросить прогресс")
        app.clickText("Продолжить")
        app.waitForText("Что будет удалено")
        app.waitForTextContaining("Отметки «Просмотрено» (2) и закладки (1) удалятся, место, где вы остановились, забудется.")
        app.assertNoText("Тема «$sampleTitle» и её файлы удалятся с телефона.")
        app.clickText("Подтвердить")

        app.waitForText("Прогресс сброшен")
        app.waitForText("Управление темой")
        assertEquals(0, viewedCount(ThemeZips.SAMPLE_ID))
        assertEquals(emptyList<String>(), bookmarkTitles())
        assertEquals(null, readingPoint())
        assertTrue(themeFolder(ThemeZips.SAMPLE_ID).exists())
        app.onNodeWithText("Продолжить").assertIsNotEnabled()
    }

    @Test
    fun cancelInRemovalSheetChangesNothing() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        app.openThemeManagement(sampleTitle)
        app.clickText("Удалить тему")
        app.clickText("Продолжить")
        app.waitForText("Что будет удалено")

        app.clickText("Отмена")

        app.waitForGone("Что будет удалено")
        assertTrue(themeFolder(ThemeZips.SAMPLE_ID).exists())
        assertEquals(1, viewedCount(ThemeZips.SAMPLE_ID))
    }

    @Test
    fun deleteOtherThemeKeepsProgressAndReturnsToList() {
        app.passOnboarding()
        installOtherTheme()
        markViewed("other-theme", "welcome")
        app.clickText("Управлять")
        app.waitForText("Управление темой")

        app.clickText("Удалить тему")
        app.clickText("Продолжить")
        app.waitForTextContaining("Тема «$otherTitle» и её файлы удалятся с телефона.")
        app.waitForTextContaining("Отметки «Просмотрено» (1) и закладки (0) останутся и вернутся")
        app.clickText("Подтвердить")

        app.waitForText("Добавить тему")
        app.assertNoText(otherTitle)
        assertFalse(themeFolder("other-theme").exists())
        assertEquals(1, viewedCount("other-theme"))
    }

    @Test
    fun deleteWithResetClearsProgressToo() {
        app.passOnboarding()
        installOtherTheme()
        markViewed("other-theme", "welcome")
        app.clickText("Управлять")

        app.clickText("Удалить тему")
        app.clickText("Сбросить прогресс")
        app.clickText("Продолжить")
        app.waitForTextContaining("Отметки «Просмотрено» (1) и закладки (0) удалятся")
        app.clickText("Подтвердить")

        app.waitForText("Добавить тему")
        assertEquals(0, viewedCount("other-theme"))
    }

    @Test
    fun deletingCurrentThemeSwitchesToLastUsedOne() {
        app.passOnboarding()
        installOtherTheme()
        applyOtherTheme()
        app.openThemeManagement(otherTitle)

        app.clickText("Удалить тему")
        app.clickText("Продолжить")
        app.waitForTextContaining("Откроется последняя использованная тема")
        app.clickText("Подтвердить")

        app.waitForContentsOf(sampleTitle)
        app.assertNoText(otherTitle)
        Espresso.pressBackUnconditionally()
        app.activityRule.scenario.waitForDestroyed()
    }

    @Test
    fun deletingCurrentThemeOpensMostRecentlyUsedNotFirstByTitle() {
        val laterTitle = "Яблочная тема"
        app.passOnboarding()
        installTheme("/themes/later.zip", "later-theme", laterTitle)
        applyTheme(laterTitle)
        installOtherTheme()
        applyOtherTheme()
        app.openThemeManagement(otherTitle)

        app.clickText("Удалить тему")
        app.clickText("Продолжить")
        app.clickText("Подтвердить")

        app.waitForContentsOf(laterTitle)
    }

    @Test
    fun deletingLastThemeReturnsToFirstThemeScreen() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        app.openThemeManagement(sampleTitle)

        app.clickText("Удалить тему")
        app.clickText("Продолжить")
        app.clickText("Подтвердить")

        app.waitForText("Добавить по ссылке")
        app.waitForText("Начать с примера")
        assertFalse(themeFolder(ThemeZips.SAMPLE_ID).exists())
        assertEquals(1, viewedCount(ThemeZips.SAMPLE_ID))
    }

    @Test
    fun deletedThemeReturnedLaterGetsItsProgressBack() {
        app.passOnboarding()
        markViewed(ThemeZips.SAMPLE_ID, "welcome")
        addBookmark("welcome", "Закладка")
        app.openThemeManagement(sampleTitle)
        setReadingPoint("headings")
        app.clickText("Удалить тему")
        app.clickText("Продолжить")
        app.waitForTextContaining("Отметки «Просмотрено» (1) и закладки (1) останутся и вернутся")
        app.clickText("Подтвердить")
        app.waitForText("Начать с примера")
        assertEquals("headings", readingPoint()?.nodeId)

        app.clickText("Начать с примера")
        app.installFromPreview(sampleTitle)

        app.waitForContentsOf(sampleTitle)
        assertEquals(1, viewedCount(ThemeZips.SAMPLE_ID))
        assertEquals(listOf("Закладка"), bookmarkTitles())
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        const val SERVER_ERROR = 500
        const val REPLACE_QUESTION = "Заменить источник и содержимое темы «${ThemeZips.SAMPLE_TITLE}»? " +
            "Отметки «Просмотрено» и закладки сохранятся у страниц и разделов, которые остались в теме."
    }
}
