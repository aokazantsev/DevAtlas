package ru.aokazantsev.devatlas.reading.domain

import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint

interface ReadingPointRepository {
    fun record(point: ReadingPoint)

    suspend fun last(themeId: String): ReadingPoint?
}
