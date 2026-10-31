package ru.aokazantsev.devatlas.update.domain

import ru.aokazantsev.devatlas.update.domain.model.ReleasesResult

interface ReleasesRepository {
    suspend fun load(): ReleasesResult
}
