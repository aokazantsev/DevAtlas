package ru.aokazantsev.devatlas.reading

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.aokazantsev.devatlas.reading.domain.BookmarkRepository
import ru.aokazantsev.devatlas.reading.domain.ReadingPointRepository
import ru.aokazantsev.devatlas.reading.domain.model.Bookmark
import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.settings.domain.ReadingSettingsRepository

class FakeBookmarkRepository : BookmarkRepository {
    private val state = MutableStateFlow<Map<String, List<Bookmark>>>(emptyMap())
    private var nextId = 1L
    val reorderGates = ArrayDeque<CompletableDeferred<Unit>>()

    fun of(themeId: String): List<Bookmark> = state.value[themeId].orEmpty()

    override fun observe(themeId: String): Flow<List<Bookmark>> = state.map { it[themeId].orEmpty() }

    override suspend fun add(themeId: String, target: ScreenTarget, anchor: Int, title: String) {
        val bookmark = Bookmark(id = nextId++, target = target, anchor = anchor, title = title)
        state.value += themeId to of(themeId) + bookmark
    }

    override suspend fun rename(id: Long, title: String) {
        state.value = state.value.mapValues { (_, list) -> list.map { if (it.id == id) it.copy(title = title) else it } }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.mapValues { (_, list) -> list.filterNot { it.id == id } }
    }

    override suspend fun reorder(themeId: String, orderedIds: List<Long>) {
        reorderGates.removeFirstOrNull()?.await()
        val rank = orderedIds.withIndex().associate { (index, id) -> id to index }
        state.value += themeId to of(themeId).sortedBy { rank[it.id] ?: Int.MAX_VALUE }
    }
}

class FakeReadingPointRepository : ReadingPointRepository {
    val points = mutableMapOf<String, ReadingPoint>()
    val recorded = mutableListOf<ReadingPoint>()

    override fun record(point: ReadingPoint) {
        recorded += point
        points[point.themeId] = point
    }

    override suspend fun last(themeId: String): ReadingPoint? = points[themeId]
}

class FakeReadingSettings(enabled: Boolean = true) : ReadingSettingsRepository {
    override val continueFromLastPoint = MutableStateFlow(enabled)

    override suspend fun setContinueFromLastPoint(enabled: Boolean) {
        continueFromLastPoint.value = enabled
    }
}
