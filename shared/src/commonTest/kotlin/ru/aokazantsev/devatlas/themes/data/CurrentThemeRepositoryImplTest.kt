package ru.aokazantsev.devatlas.themes.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException
import ru.aokazantsev.devatlas.settings.data.FailingPreferencesDataStore
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrentThemeRepositoryImplTest {

    @Test
    fun currentThemeIsSetAndCleared() = runTest {
        val repository = CurrentThemeRepositoryImpl(InMemoryPreferencesDataStore())

        repository.setCurrent("a")
        assertEquals("a", repository.currentThemeId.first())

        repository.setCurrent(null)
        assertNull(repository.currentThemeId.first())
    }

    @Test
    fun readIoErrorGivesNoCurrentTheme() = runTest {
        assertNull(CurrentThemeRepositoryImpl(FailingPreferencesDataStore(IOException("disk"))).currentThemeId.first())
    }

    @Test
    fun writeIoErrorIsSwallowed() = runTest {
        CurrentThemeRepositoryImpl(FailingPreferencesDataStore(IOException("disk"))).setCurrent("a")
    }
}
