package ru.aokazantsev.devatlas.usage.data

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.onboarding.data.OnboardingRepositoryImpl
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import ru.aokazantsev.devatlas.settings.data.ThemeSettingsRepositoryImpl
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.update.data.UpdateSettingsRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageStateSourceImplTest {

    private val dataStore = InMemoryPreferencesDataStore()
    private val onboarding = OnboardingRepositoryImpl(dataStore)
    private val themeSettings = ThemeSettingsRepositoryImpl(dataStore)
    private val installed = FakeInstalledThemeRepository()

    private fun source(currentThemeId: String?) = UsageStateSourceImpl(
        onboarding = onboarding,
        updateSettings = UpdateSettingsRepositoryImpl(dataStore),
        themeSettings = themeSettings,
        current = FakeCurrentThemeRepository(currentThemeId),
        installed = installed,
    )

    @Test
    fun stateKeysMatchSiteCatalog() = runTest {
        onboarding.accept(crashReports = true, usageStatistics = true)
        themeSettings.setThemeMode(ThemeMode.DARK)
        themeSettings.setWallpaperColors(false)
        installed.save(installedOf("devatlas-sample"))
        installed.save(installedOf("my-theme"))

        val state = source("devatlas-sample").state()

        assertEquals(
            mapOf(
                "crash_reports" to "true",
                "auto_update" to "true",
                "theme_mode" to "dark",
                "wallpaper_colors" to "false",
                "themes_count" to "2",
                "theme" to "sample",
            ),
            state,
        )
    }

    @Test
    fun ownThemeIsCustomAndBrokenDatabaseSkipsCount() = runTest {
        installed.databaseFailure = SQLiteException("broken")

        val state = source("my-theme").state()

        assertEquals("custom", state["theme"])
        assertNull(state["themes_count"])
    }

    @Test
    fun roomIntegrityFailureSkipsCountInsteadOfCrashing() = runTest {
        installed.databaseFailure = IllegalStateException("Room cannot verify the data integrity")

        assertNull(source("my-theme").state()["themes_count"])
    }

    @Test
    fun noCurrentThemeSendsNoTheme() = runTest {
        assertNull(source(null).state()["theme"])
    }
}
