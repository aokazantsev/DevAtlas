package ru.aokazantsev.devatlas.reading.domain

import ru.aokazantsev.devatlas.reading.domain.model.Continuation

interface FindContinuationUseCase {
    suspend operator fun invoke(themeId: String): Continuation?
}
