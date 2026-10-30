package ru.aokazantsev.devatlas.themes.domain

interface ProgressRepository {
    suspend fun viewedCount(themeId: String): Int

    suspend fun reset(themeId: String)

    suspend fun keepOnly(themeId: String, pageIds: Collection<String>)
}
