package ru.aokazantsev.devatlas.reading.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.aokazantsev.devatlas.database.data.dao.BookmarkDao
import ru.aokazantsev.devatlas.database.data.entity.BookmarkEntity
import ru.aokazantsev.devatlas.reading.domain.BookmarkRepository
import ru.aokazantsev.devatlas.reading.domain.model.Bookmark
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget

@ContributesBinding(AppScope::class)
@Inject
internal class BookmarkRepositoryImpl(
    private val dao: BookmarkDao,
) : BookmarkRepository {

    override fun observe(themeId: String): Flow<List<Bookmark>> = dao.observe(themeId).map { entities ->
        entities.mapNotNull { entity ->
            ScreenTargetColumns.target(entity.kind, entity.nodeId)?.let { target ->
                Bookmark(id = entity.id, target = target, anchor = entity.anchor, title = entity.title)
            }
        }
    }

    override suspend fun add(themeId: String, target: ScreenTarget, anchor: Int, title: String) {
        dao.insert(
            BookmarkEntity(
                themeId = themeId,
                kind = ScreenTargetColumns.kind(target),
                nodeId = ScreenTargetColumns.nodeId(target),
                anchor = anchor,
                title = title,
                position = dao.nextPosition(themeId),
            ),
        )
    }

    override suspend fun rename(id: Long, title: String) {
        dao.rename(id, title)
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    override suspend fun reorder(themeId: String, orderedIds: List<Long>) {
        val rank = orderedIds.withIndex().associate { (index, id) -> id to index }
        val sorted = dao.all(themeId).sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        dao.update(sorted.mapIndexed { position, entity -> entity.copy(position = position) })
    }
}
