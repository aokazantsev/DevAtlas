package ru.aokazantsev.devatlas.settings.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThemeSettingsRepositoryImplTest {

    @Test
    fun emptyStoreGivesSystemWithWallpaperColors() = runTest {
        val appearance = ThemeSettingsRepositoryImpl(InMemoryPreferencesDataStore()).appearance.first()

        assertEquals(Appearance(ThemeMode.SYSTEM, wallpaperColors = true), appearance)
    }

    @Test
    fun unknownValueGivesSystem() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "PURPLE" }

        assertEquals(ThemeMode.SYSTEM, ThemeSettingsRepositoryImpl(dataStore).appearance.first().themeMode)
    }

    @Test
    fun storedModeIsReadBack() = runTest {
        val repository = ThemeSettingsRepositoryImpl(InMemoryPreferencesDataStore())

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(Appearance(ThemeMode.DARK, wallpaperColors = true), repository.appearance.first())
    }

    @Test
    fun wallpaperColorsOffIsReadBackWithoutTouchingMode() = runTest {
        val repository = ThemeSettingsRepositoryImpl(InMemoryPreferencesDataStore())
        repository.setThemeMode(ThemeMode.LIGHT)

        repository.setWallpaperColors(false)

        assertEquals(Appearance(ThemeMode.LIGHT, wallpaperColors = false), repository.appearance.first())
    }

    @Test
    fun ioErrorGivesDefault() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))

        assertEquals(Appearance.Default, repository.appearance.first())
    }

    @Test
    fun writeIoErrorIsSwallowed() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))

        repository.setThemeMode(ThemeMode.DARK)
        repository.setWallpaperColors(false)
    }

    @Test
    fun writeOtherErrorPropagates() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IllegalStateException("bug")))

        assertFailsWith<IllegalStateException> { repository.setWallpaperColors(false) }
    }

    @Test
    fun otherErrorPropagates() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IllegalStateException("bug")))

        assertFailsWith<IllegalStateException> { repository.appearance.first() }
    }
}
