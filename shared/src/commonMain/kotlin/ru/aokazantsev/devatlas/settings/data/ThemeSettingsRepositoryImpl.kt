package ru.aokazantsev.devatlas.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class ThemeSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ThemeSettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
        ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE_KEY] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        try {
            dataStore.edit { it[THEME_MODE_KEY] = mode.name }
        } catch (_: IOException) {
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
