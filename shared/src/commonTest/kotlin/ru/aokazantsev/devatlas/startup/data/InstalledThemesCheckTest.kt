package ru.aokazantsev.devatlas.startup.data

import kotlinx.coroutines.test.runTest
import okio.FileMetadata
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import ru.aokazantsev.devatlas.themes.data.ThemePackageReaderImpl
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.installedOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InstalledThemesCheckTest {

    private val fileSystem = FakeFileSystem()
    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository()
    private val storage = FakeThemeStorage()

    private val check = InstalledThemesCheck(repository, current, storage, ThemePackageReaderImpl(fileSystem), fileSystem)

    private fun writeTheme(id: String, manifestId: String = id, page: String? = "<p>Привет</p>") {
        val root = "/themes/$id".toPath()
        fileSystem.createDirectories(root / "pages")
        val manifest = """{"formatVersion":1,"id":"$manifestId","title":"Тема","tree":[""" +
            """{"id":"s","title":"Раздел","children":[{"id":"p1","title":"П","file":"pages/p1.html"}]}]}"""
        fileSystem.write(root / "manifest.json") { writeUtf8(manifest) }
        if (page != null) fileSystem.write(root / "pages/p1.html") { writeUtf8(page) }
    }

    @Test
    fun noThemesPassesAndClearsStaging() = runTest {
        assertEquals(StartupCheckResult.Passed, check.check())
        assertEquals(1, storage.cleared)
    }

    @Test
    fun databaseFailureIsShownAsStartupError() = runTest {
        repository.databaseFailure = androidx.sqlite.SQLiteException("database disk image is malformed")

        assertIs<StartupCheckResult.Failed>(check.check())
    }

    @Test
    fun roomIllegalStateIsShownAsStartupError() = runTest {
        repository.databaseFailure = IllegalStateException("Room cannot verify the data integrity")

        assertEquals(StartupCheckResult.Failed("Room cannot verify the data integrity"), check.check())
    }

    @Test
    fun fileSystemFailureIsShownAsStartupError() = runTest {
        repository.save(installedOf("a"))
        val failingDisk = object : ForwardingFileSystem(fileSystem) {
            override fun metadataOrNull(path: Path): FileMetadata? = throw IOException("I/O error")
        }
        val failingCheck = InstalledThemesCheck(repository, current, storage, ThemePackageReaderImpl(failingDisk), failingDisk)

        assertEquals(StartupCheckResult.Failed("I/O error"), failingCheck.check())
    }

    @Test
    fun healthyThemesPass() = runTest {
        repository.save(installedOf("a"))
        writeTheme("a")

        assertEquals(StartupCheckResult.Passed, check.check())
    }

    @Test
    fun missingFolderIsDamage() = runTest {
        repository.save(installedOf("a"))

        val result = assertIs<StartupCheckResult.ThemeDamaged>(check.check())

        assertEquals("a", result.theme.themeId)
    }

    @Test
    fun brokenPageIsDamage() = runTest {
        repository.save(installedOf("a"))
        writeTheme("a", page = null)

        val result = assertIs<StartupCheckResult.ThemeDamaged>(check.check())

        assertEquals("Пропал файл страницы «pages/p1.html»", result.theme.reason)
    }

    @Test
    fun formatRulesTightenedLaterDoNotMarkInstalledThemeDamaged() = runTest {
        repository.save(installedOf("a"))
        writeTheme("a", page = "<script>старое правило</script><p>ok</p>")
        fileSystem.write("/themes/a/pages/x.svg".toPath()) { writeUtf8("<svg><animate attributeName=\"fill\"/></svg>") }

        assertEquals(StartupCheckResult.Passed, check.check())
    }

    @Test
    fun brokenManifestIsDamage() = runTest {
        repository.save(installedOf("a"))
        writeTheme("a")
        fileSystem.write("/themes/a/manifest.json".toPath()) { writeUtf8("{ не json") }

        val result = assertIs<StartupCheckResult.ThemeDamaged>(check.check())

        assertEquals("Файл manifest.json темы испорчен", result.theme.reason)
    }

    @Test
    fun foreignManifestIdIsDamage() = runTest {
        repository.save(installedOf("a"))
        writeTheme("a", manifestId = "other")

        val result = assertIs<StartupCheckResult.ThemeDamaged>(check.check())

        assertEquals("Файлы темы относятся к другой теме", result.theme.reason)
    }

    @Test
    fun currentDamagedThemeIsReportedFirst() = runTest {
        repository.save(installedOf("a", lastUsedAt = 5L))
        repository.save(installedOf("b", lastUsedAt = 1L))
        current.setCurrent("b")

        val result = assertIs<StartupCheckResult.ThemeDamaged>(check.check())

        assertEquals("b", result.theme.themeId)
    }
}
