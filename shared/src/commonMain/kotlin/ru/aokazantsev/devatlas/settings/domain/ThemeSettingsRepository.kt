package ru.aokazantsev.devatlas.settings.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode

interface ThemeSettingsRepository {
    val appearance: Flow<Appearance>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setWallpaperColors(enabled: Boolean)
}
