package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.flow.Flow

interface CurrentThemeRepository {
    val currentThemeId: Flow<String?>

    suspend fun setCurrent(themeId: String?)
}
