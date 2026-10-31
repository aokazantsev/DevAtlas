package ru.aokazantsev.devatlas.update.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.IOException
import ru.aokazantsev.devatlas.update.domain.UpdateSettingsRepository

@ContributesBinding(AppScope::class)
@Inject
internal class UpdateSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UpdateSettingsRepository {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    override val autoCheck: Flow<Boolean> = preferences.map { it[AUTO_CHECK_KEY] ?: true }

    override suspend fun setAutoCheck(enabled: Boolean) = edit { it[AUTO_CHECK_KEY] = enabled }

    override suspend fun lastAutoCheckMillis(): Long = preferences.first()[LAST_CHECK_KEY] ?: 0L

    override suspend fun setLastAutoCheckMillis(millis: Long) = edit { it[LAST_CHECK_KEY] = millis }

    override suspend fun announcedVersion(): String? = preferences.first()[ANNOUNCED_KEY]

    override suspend fun setAnnouncedVersion(version: String) = edit { it[ANNOUNCED_KEY] = version }

    private suspend fun edit(change: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit(change)
        } catch (_: IOException) {
        }
    }

    private companion object {
        val AUTO_CHECK_KEY = booleanPreferencesKey("update_auto_check")
        val LAST_CHECK_KEY = longPreferencesKey("update_last_auto_check")
        val ANNOUNCED_KEY = stringPreferencesKey("update_announced_version")
    }
}
