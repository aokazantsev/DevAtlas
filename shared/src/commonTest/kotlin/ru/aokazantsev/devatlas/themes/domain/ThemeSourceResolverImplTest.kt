package ru.aokazantsev.devatlas.themes.domain

import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themes.domain.model.ArchiveLocation
import ru.aokazantsev.devatlas.themes.domain.model.SourceResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeSourceResolverImplTest {

    private val resolver = ThemeSourceResolverImpl(NetworkEndpoints.Production)

    private fun plan(input: String) = assertIs<SourceResolution.Resolved>(resolver.resolve(input)).plan

    private fun rejection(input: String) = assertIs<SourceResolution.Rejected>(resolver.resolve(input)).message

    @Test
    fun yandexFolderUsesPublicKey() {
        val result = plan("https://disk.yandex.ru/d/AbC123")

        assertEquals(ArchiveLocation.YandexPublic("https://disk.yandex.ru/d/AbC123", null), result.location)
    }

    @Test
    fun yandexFolderSubpathBecomesPathParameter() {
        val result = plan("https://disk.yandex.ru/d/AbC123/themes/android")

        assertEquals(ArchiveLocation.YandexPublic("https://disk.yandex.ru/d/AbC123", "/themes/android"), result.location)
    }

    @Test
    fun yandexFileAndShortHostAreAccepted() {
        assertEquals(
            ArchiveLocation.YandexPublic("https://disk.yandex.ru/i/FileKey", null),
            plan("https://disk.yandex.ru/i/FileKey").location,
        )
        assertEquals(ArchiveLocation.YandexPublic("https://yadi.sk/d/Key", null), plan("https://yadi.sk/d/Key").location)
    }

    @Test
    fun yandexNonPublicPageIsRejected() {
        assertTrue(rejection("https://disk.yandex.ru/client/disk").contains("публичная"))
    }

    @Test
    fun githubRepositoryUsesHeadOfDefaultBranch() {
        val result = plan("https://github.com/owner/repo")

        assertEquals(ArchiveLocation.Direct("https://codeload.github.com/owner/repo/zip/HEAD"), result.location)
        assertNull(result.subFolder)
    }

    @Test
    fun githubTreeGivesBranchAndSubFolder() {
        val result = plan("https://github.com/owner/repo/tree/main/themes/android")

        assertEquals(ArchiveLocation.Direct("https://codeload.github.com/owner/repo/zip/main"), result.location)
        assertEquals("themes/android", result.subFolder)
    }

    @Test
    fun githubGitSuffixIsStripped() {
        assertEquals(
            ArchiveLocation.Direct("https://codeload.github.com/owner/repo/zip/HEAD"),
            plan("https://github.com/owner/repo.git").location,
        )
    }

    @Test
    fun githubOtherPagesAreRejected() {
        assertTrue(rejection("https://github.com/owner").contains("репозиторий"))
        assertTrue(rejection("https://github.com/owner/repo/issues/1").contains("репозиторий"))
        assertTrue(rejection("https://github.com/owner/repo/tree/main/../x").contains("репозиторий"))
        assertTrue(rejection("https://github.com/owner/repo/tree/main/%2E%2E/x").contains("репозиторий"))
        assertTrue(rejection("https://github.com/owner/repo/tree/main/a%2Fb").contains("репозиторий"))
        assertTrue(rejection("https://github.com/owner/repo/tree/../x").contains("репозиторий"))
        assertTrue(rejection("https://github.com/owner/repo/tree/main/%D0").contains("репозиторий"))
    }

    @Test
    fun githubFileViewLinkToZipDownloadsRawFile() {
        val result = plan("https://github.com/owner/repo/blob/main/themes/theme.zip")

        assertEquals(ArchiveLocation.Direct("https://github.com/owner/repo/raw/main/themes/theme.zip"), result.location)
        assertNull(result.subFolder)
    }

    @Test
    fun githubFileViewLinkToOtherFileIsRejected() {
        assertTrue(rejection("https://github.com/owner/repo/blob/main/README.md").contains("репозиторий"))
    }

    @Test
    fun encodedFolderNamesAreDecoded() {
        assertEquals("Моя тема", plan("https://github.com/owner/repo/tree/main/%D0%9C%D0%BE%D1%8F%20%D1%82%D0%B5%D0%BC%D0%B0").subFolder)
        assertEquals("Моя тема", plan("https://github.com/owner/repo/tree/main/Моя%20тема").subFolder)
        assertEquals(
            ArchiveLocation.YandexPublic("https://disk.yandex.ru/d/Key", "/Темы/android"),
            plan("https://disk.yandex.ru/d/Key/%D0%A2%D0%B5%D0%BC%D1%8B/android").location,
        )
    }

    @Test
    fun brokenEscapeInYandexPathIsRejected() {
        assertTrue(rejection("https://disk.yandex.ru/d/Key/%ZZ").contains("ссылку"))
        assertTrue(rejection("https://disk.yandex.ru/d/Key/..").contains("ссылку"))
        assertTrue(rejection("https://disk.yandex.ru/d/Key/%2E%2E/x").contains("ссылку"))
        assertTrue(rejection("https://disk.yandex.ru/d/Key/a%2Fb").contains("ссылку"))
        assertTrue(rejection("https://disk.yandex.ru/d/Key/%D0").contains("ссылку"))
    }

    @Test
    fun anyHttpsZipIsDirect() {
        val result = plan("https://example.com/files/theme.ZIP?download=1")

        assertEquals(ArchiveLocation.Direct("https://example.com/files/theme.ZIP?download=1"), result.location)
    }

    @Test
    fun fragmentIsDroppedFromAddress() {
        assertEquals("https://example.com/theme.zip", plan("https://example.com/theme.zip#top").address)
    }

    @Test
    fun githubArchiveZipLinkIsDirect() {
        assertEquals(
            ArchiveLocation.Direct("https://github.com/owner/repo/archive/refs/heads/main.zip"),
            plan("https://github.com/owner/repo/archive/refs/heads/main.zip").location,
        )
    }

    @Test
    fun httpIsRejected() {
        assertTrue(rejection("http://example.com/theme.zip").contains("https"))
    }

    @Test
    fun httpIsAllowedOnlyWithCleartextEndpoints() {
        val lenient = ThemeSourceResolverImpl(NetworkEndpoints.Production.copy(allowCleartext = true))

        assertIs<SourceResolution.Resolved>(lenient.resolve("http://127.0.0.1:8080/theme.zip"))
    }

    @Test
    fun dropboxIsRejected() {
        assertTrue(rejection("https://www.dropbox.com/s/abc/theme.zip?dl=0").contains("Dropbox"))
    }

    @Test
    fun googleDriveFileLinksGiveFileId() {
        val expected = ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345")
        assertEquals(expected, plan("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view?usp=sharing").location)
        assertEquals(expected, plan("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345").location)
        assertEquals(expected, plan("https://drive.google.com/open?id=1AbCdEfGhIjKlMnOpQrStUvWxYz012345").location)
        assertEquals(expected, plan("https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz012345").location)
        assertEquals(
            expected,
            plan("https://drive.usercontent.google.com/download?id=1AbCdEfGhIjKlMnOpQrStUvWxYz012345&export=download").location,
        )
        assertNull(plan("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view").subFolder)
    }

    @Test
    fun googleDriveAccountAndLegacyFormsGiveFileId() {
        val expected = ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345")
        assertEquals(expected, plan("https://drive.google.com/file/u/0/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view").location)
        assertEquals(expected, plan("https://drive.google.com/a/example.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view").location)
        assertEquals(expected, plan("https://docs.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz012345").location)
        assertEquals(expected, plan("https://drive.google.com/a/d/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view").location)
        assertEquals(expected, plan("https://docs.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/edit").location)
    }

    @Test
    fun googleDriveResourceKeyIsKept() {
        assertEquals(
            ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345", "0-AbCdEfGhIjKl"),
            plan("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view?usp=sharing&resourcekey=0-AbCdEfGhIjKl").location,
        )
        assertEquals(
            ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345", "0-AbCdEfGhIjKl"),
            plan("https://drive.google.com/open?id=1AbCdEfGhIjKlMnOpQrStUvWxYz012345&resourcekey=0-AbCdEfGhIjKl").location,
        )
        assertEquals(
            ArchiveLocation.GoogleDriveFile("1AbCdEfGhIjKlMnOpQrStUvWxYz012345"),
            plan("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/view?resourcekey=..").location,
        )
    }

    @Test
    fun googleDriveFoldersDocumentsAndOddLinksAreRejected() {
        assertTrue(rejection("https://drive.google.com/drive/folders/1FVTUAyi_CQqIaymG9SXMAcMY5yW-Y6L0?usp=drive_link").contains("Папку"))
        assertTrue(rejection("https://drive.google.com/drive/u/0/folders/1FVTUAyi_CQqIaymG9SXMAcMY5yW-Y6L0").contains("Папку"))
        assertTrue(rejection("https://docs.google.com/document/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/edit").contains("Документы Google"))
        assertTrue(rejection("https://drive.google.com/file/d/1/view").contains("zip-файл"))
        assertTrue(rejection("https://drive.google.com/open?id=../x").contains("zip-файл"))
        assertTrue(rejection("https://drive.google.com/").contains("zip-файл"))
        assertTrue(rejection("https://docs.google.com/uc?export=download").contains("zip-файл"))
        assertTrue(rejection("https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOpQrStUvWxYz012345/edit").contains("Документы Google"))
    }

    @Test
    fun plainHttpsFolderIsRejected() {
        assertTrue(rejection("https://example.com/themes/").contains("Не понимаю"))
    }

    @Test
    fun garbageIsRejected() {
        assertEquals("Вставьте ссылку на тему", rejection("   "))
        assertTrue(rejection("not a link").contains("не только ссылка"))
        assertTrue(rejection("example.com/theme.zip").contains("не похоже"))
        assertTrue(rejection("ftp://example.com/theme.zip").contains("https"))
        assertTrue(rejection("https://user@example.com/theme.zip").contains("не похоже"))
        assertTrue(rejection("https://github.com:x@example.org/theme.zip").contains("не похоже"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("https://example.com/theme.zip", plan("  https://example.com/theme.zip\n").address)
    }

    @Test
    fun singleLinkInsideMessageIsTaken() {
        assertEquals(
            "https://disk.yandex.ru/d/AbC123",
            plan("Вот тема по Kotlin: https://disk.yandex.ru/d/AbC123.").address,
        )
        assertEquals("https://example.com/theme.zip", plan("тема (https://example.com/theme.zip)\nпробуй").address)
    }

    @Test
    fun messageWithSeveralLinksIsRejected() {
        assertTrue(rejection("https://example.com/a.zip или https://example.com/b.zip").contains("не только ссылка"))
    }
}
