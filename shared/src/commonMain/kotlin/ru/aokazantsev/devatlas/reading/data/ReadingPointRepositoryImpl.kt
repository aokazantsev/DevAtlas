package ru.aokazantsev.devatlas.reading.data

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.database.data.dao.ReadingPointDao
import ru.aokazantsev.devatlas.database.data.entity.ReadingPointEntity
import ru.aokazantsev.devatlas.reading.domain.ReadingPointRepository
import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class ReadingPointRepositoryImpl(
    private val dao: ReadingPointDao,
    scope: CoroutineScope,
) : ReadingPointRepository {

    private val pending = Channel<ReadingPoint>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (point in pending) save(point)
        }
    }

    override fun record(point: ReadingPoint) {
        pending.trySend(point)
    }

    override suspend fun last(themeId: String): ReadingPoint? {
        val entity = try {
            dao.find(themeId)
        } catch (_: SQLiteException) {
            null
        } ?: return null
        val target = ScreenTargetColumns.target(entity.kind, entity.nodeId) ?: return null
        return ReadingPoint(themeId = themeId, target = target, anchor = entity.anchor)
    }

    private suspend fun save(point: ReadingPoint) {
        try {
            dao.upsert(
                ReadingPointEntity(
                    themeId = point.themeId,
                    kind = ScreenTargetColumns.kind(point.target),
                    nodeId = ScreenTargetColumns.nodeId(point.target),
                    anchor = point.anchor,
                ),
            )
        } catch (_: SQLiteException) {
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
        }
    }
}
