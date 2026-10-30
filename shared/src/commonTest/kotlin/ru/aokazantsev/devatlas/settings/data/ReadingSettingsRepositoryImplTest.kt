package ru.aokazantsev.devatlas.settings.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingSettingsRepositoryImplTest {

    @Test
    fun continuationIsOnByDefault() = runTest {
        assertEquals(true, ReadingSettingsRepositoryImpl(InMemoryPreferencesDataStore()).continueFromLastPoint.first())
    }

    @Test
    fun switchedOffIsReadBack() = runTest {
        val repository = ReadingSettingsRepositoryImpl(InMemoryPreferencesDataStore())

        repository.setContinueFromLastPoint(false)

        assertEquals(false, repository.continueFromLastPoint.first())
    }

    @Test
    fun readErrorKeepsDefault() = runTest {
        val repository = ReadingSettingsRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))

        assertEquals(true, repository.continueFromLastPoint.first())
    }

    @Test
    fun writeErrorIsSwallowed() = runTest {
        val repository = ReadingSettingsRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))

        repository.setContinueFromLastPoint(false)
    }
}
