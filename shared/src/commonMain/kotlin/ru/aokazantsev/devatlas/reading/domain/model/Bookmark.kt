package ru.aokazantsev.devatlas.reading.domain.model

data class Bookmark(
    val id: Long,
    val target: ScreenTarget,
    val anchor: Int,
    val title: String,
)
