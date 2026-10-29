package ru.aokazantsev.devatlas.settings.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThemeSettingsRepositoryImplTest {

    @Test
    fun emptyStoreGivesSystem() = runTest {
        assertEquals(ThemeMode.SYSTEM, ThemeSettingsRepositoryImpl(InMemoryPreferencesDataStore()).themeMode.first())
    }

    @Test
    fun unknownValueGivesSystem() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "PURPLE" }

        assertEquals(ThemeMode.SYSTEM, ThemeSettingsRepositoryImpl(dataStore).themeMode.first())
    }

    @Test
    fun storedModeIsReadBack() = runTest {
        val repository = ThemeSettingsRepositoryImpl(InMemoryPreferencesDataStore())

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun ioErrorGivesSystem() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun writeIoErrorIsSwallowed() = runTest {
        ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IOException("disk"))).setThemeMode(ThemeMode.DARK)
    }

    @Test
    fun writeOtherErrorPropagates() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IllegalStateException("bug")))

        assertFailsWith<IllegalStateException> { repository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun otherErrorPropagates() = runTest {
        val repository = ThemeSettingsRepositoryImpl(FailingPreferencesDataStore(IllegalStateException("bug")))

        assertFailsWith<IllegalStateException> { repository.themeMode.first() }
    }
}
