package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval

interface ThemeLibrary {
    fun observeInstalled(): Flow<List<InstalledTheme>>

    fun observeCurrent(): Flow<InstalledTheme?>

    suspend fun resolveCurrent(): InstalledTheme?

    suspend fun apply(themeId: String)

    suspend fun viewedCount(themeId: String): Int

    suspend fun bookmarkCount(themeId: String): Int

    suspend fun remove(themeId: String, deleteTheme: Boolean, resetProgress: Boolean): ThemeRemoval
}
