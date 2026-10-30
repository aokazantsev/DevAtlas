package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeViewed(themeId: String): Flow<Set<String>>

    suspend fun setViewed(themeId: String, pageId: String, viewed: Boolean)

    suspend fun viewedCount(themeId: String): Int

    suspend fun reset(themeId: String)

    suspend fun keepOnly(themeId: String, pageIds: Collection<String>)
}
