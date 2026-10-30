package ru.aokazantsev.devatlas.reading.domain.model

data class ReadingPoint(
    val themeId: String,
    val target: ScreenTarget,
    val anchor: Int,
)
