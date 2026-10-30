package ru.aokazantsev.devatlas.themes.data

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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException
import ru.aokazantsev.devatlas.themes.domain.CurrentThemeRepository

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class CurrentThemeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : CurrentThemeRepository {

    override val currentThemeId: Flow<String?> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[CURRENT_THEME_KEY] }
        .distinctUntilChanged()

    override suspend fun setCurrent(themeId: String?) {
        try {
            dataStore.edit { preferences ->
                if (themeId == null) preferences.remove(CURRENT_THEME_KEY) else preferences[CURRENT_THEME_KEY] = themeId
            }
        } catch (_: IOException) {
        }
    }

    private companion object {
        val CURRENT_THEME_KEY = stringPreferencesKey("current_theme_id")
    }
}
