package ru.aokazantsev.devatlas.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeSourcesE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    private val title = ThemeZips.SAMPLE_TITLE

    private fun installFrom(address: String) {
        val app = e2e.compose
        app.acceptPolicy()
        app.installThemeByAddress(address, title)
        app.waitForContentsOf(title)
    }

    @Test
    fun yandexFolderIsDownloadedThroughPublicApi() {
        val key = "https://disk.yandex.ru/d/folderKey"
        e2e.server.serveYandexFolder(key, "/yandex-files/folder.zip", ThemeZips.sampleZip())

        installFrom(key)

        assertTrue(MockServerRule.YANDEX_API_PATH in e2e.server.requestedPaths)
        assertTrue("/yandex-files/folder.zip" in e2e.server.requestedPaths)
    }

    @Test
    fun yandexZipFileIsDownloadedThroughSameApi() {
        val key = "https://disk.yandex.ru/i/fileKey"
        e2e.server.serveYandexFolder(key, "/yandex-files/file.zip", ThemeZips.sampleZip())

        installFrom(key)

        assertTrue("/yandex-files/file.zip" in e2e.server.requestedPaths)
    }

    @Test
    fun yandexSubfolderIsPassedDecodedAsPath() {
        val key = "https://disk.yandex.ru/d/folderKey"
        e2e.server.serveYandexFolder(key, "/yandex-files/sub.zip", ThemeZips.sampleZip(), folder = "/Темы/android")

        installFrom("$key/%D0%A2%D0%B5%D0%BC%D1%8B/android")

        assertTrue("/yandex-files/sub.zip" in e2e.server.requestedPaths)
    }

    @Test
    fun yandexShortHostIsRecognised() {
        val key = "https://yadi.sk/d/shortKey"
        e2e.server.serveYandexFolder(key, "/yandex-files/short.zip", ThemeZips.sampleZip())

        installFrom(key)
    }

    @Test
    fun yandexArchiveWithSingleTopFolderIsAccepted() {
        val key = "https://disk.yandex.ru/d/wrapped"
        e2e.server.serveYandexFolder(key, "/yandex-files/wrapped.zip", ThemeZips.zip(ThemeZips.sampleFiles(), rootFolder = "Моя тема/"))

        installFrom(key)
    }

    @Test
    fun githubRepositoryTakesDefaultBranchArchive() {
        e2e.server.serveZip("/codeload/owner/repo/zip/HEAD", ThemeZips.sampleZipInRepositoryFolder())

        installFrom("https://github.com/owner/repo")

        assertTrue("/codeload/owner/repo/zip/HEAD" in e2e.server.requestedPaths)
    }

    @Test
    fun githubFolderTakesBranchAndSubfolder() {
        e2e.server.serveZip("/codeload/owner/repo/zip/dev", ThemeZips.sampleZipInRepositorySubfolder("themes/sample"))

        installFrom("https://github.com/owner/repo/tree/dev/themes/sample")

        assertTrue("/codeload/owner/repo/zip/dev" in e2e.server.requestedPaths)
    }

    @Test
    fun githubEncodedFolderNameIsDecoded() {
        e2e.server.serveZip("/codeload/owner/repo/zip/dev", ThemeZips.sampleZipInRepositorySubfolder("Моя тема"))

        installFrom("https://github.com/owner/repo/tree/dev/%D0%9C%D0%BE%D1%8F%20%D1%82%D0%B5%D0%BC%D0%B0")
    }

    @Test
    fun githubFolderMissingInArchiveIsReported() {
        e2e.server.serveZip("/codeload/owner/repo/zip/main", ThemeZips.sampleZipInRepositorySubfolder("themes/sample"))
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress("https://github.com/owner/repo/tree/main/themes/other")

        app.waitForTextContaining("Не найден manifest.json в папке «themes/other»")
    }

    @Test
    fun anyHttpsZipLinkIsDownloadedDirectly() {
        e2e.server.serveZip("/files/theme.zip", ThemeZips.sampleZip())

        installFrom(e2e.server.baseUrl + "/files/theme.zip")
    }

    @Test
    fun zipLinkWithQueryAndFragmentWorks() {
        e2e.server.serveZip("/files/theme.zip", ThemeZips.sampleZip())

        installFrom(e2e.server.baseUrl + "/files/theme.zip?download=1#top")
    }

    @Test
    fun closedYandexLinkExplainsWhat() {
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress("https://disk.yandex.ru/d/closed")

        app.waitForTextContaining("Яндекс.Диск не нашёл эту ссылку")
        app.waitForText("Повторить")
    }

    @Test
    fun googleDriveIsRejectedWithText() {
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress("https://drive.google.com/file/d/123/view")

        app.waitForTextContaining("Google Диск не поддерживается")
    }

    @Test
    fun plainHttpsFolderIsRejectedWithText() {
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress("https://example.com/themes/")

        app.waitForTextContaining("Обычные папки на сайтах не поддерживаются")
    }

    @Test
    fun nonPublicYandexPageIsRejectedWithText() {
        val app = e2e.compose
        app.acceptPolicy()

        app.checkAddress("https://disk.yandex.ru/client/disk")

        app.waitForTextContaining("Нужна публичная ссылка")
    }

    @Test
    fun rejectionOffersHelpPageAndEditing() {
        val app = e2e.compose
        app.acceptPolicy()
        app.checkAddress("https://drive.google.com/file/d/123/view")
        app.waitForText("Что исправить")

        app.clickText("Изменить ссылку")

        app.waitForText("Добавить по ссылке")
        app.waitForText("https://drive.google.com/file/d/123/view")
    }
}
