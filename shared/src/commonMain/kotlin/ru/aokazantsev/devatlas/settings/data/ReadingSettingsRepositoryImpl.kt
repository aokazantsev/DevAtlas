package ru.aokazantsev.devatlas.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import ru.aokazantsev.devatlas.settings.domain.ReadingSettingsRepository

@ContributesBinding(AppScope::class)
@Inject
internal class ReadingSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ReadingSettingsRepository {

    override val continueFromLastPoint: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[CONTINUE_KEY] ?: true }

    override suspend fun setContinueFromLastPoint(enabled: Boolean) {
        try {
            dataStore.edit { it[CONTINUE_KEY] = enabled }
        } catch (_: IOException) {
        }
    }

    private companion object {
        val CONTINUE_KEY = booleanPreferencesKey("continue_from_last_point")
    }
}
