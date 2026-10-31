package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

interface InstalledThemeRepository {
    fun observeAll(): Flow<List<InstalledTheme>>

    suspend fun all(): List<InstalledTheme>

    suspend fun find(themeId: String): InstalledTheme?

    suspend fun save(theme: InstalledTheme)

    suspend fun delete(themeId: String)

    suspend fun touch(themeId: String, now: Long)

    suspend fun updateValidators(themeId: String, sha256: String, etag: String?, lastModified: String?)
}
