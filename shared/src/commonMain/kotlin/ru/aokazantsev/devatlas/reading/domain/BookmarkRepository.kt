package ru.aokazantsev.devatlas.reading.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.reading.domain.model.Bookmark
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget

interface BookmarkRepository {
    fun observe(themeId: String): Flow<List<Bookmark>>

    suspend fun add(themeId: String, target: ScreenTarget, anchor: Int, title: String)

    suspend fun rename(id: Long, title: String)

    suspend fun delete(id: Long)

    suspend fun reorder(themeId: String, orderedIds: List<Long>)
}
