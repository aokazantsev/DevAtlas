package ru.aokazantsev.devatlas.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FailingPreferencesDataStore(private val error: Throwable) : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow { throw error }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw error
}
