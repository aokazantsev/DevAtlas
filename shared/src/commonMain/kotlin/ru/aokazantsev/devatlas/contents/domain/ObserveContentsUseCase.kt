package ru.aokazantsev.devatlas.contents.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents

interface ObserveContentsUseCase {
    operator fun invoke(): Flow<ThemeContents>
}
