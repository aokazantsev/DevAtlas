package ru.aokazantsev.devatlas.themes.domain

import okio.Path
import ru.aokazantsev.devatlas.themes.domain.model.StagingArea

interface ThemeStorage {
    fun newStaging(): StagingArea

    fun themeDirectory(themeId: String): Path

    suspend fun commit(staging: StagingArea, themeId: String)

    suspend fun rollback(staging: StagingArea, themeId: String)

    fun discard(staging: StagingArea)

    suspend fun deleteTheme(themeId: String)

    fun clearStaging()
}
