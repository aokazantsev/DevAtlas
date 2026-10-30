package ru.aokazantsev.devatlas.reading.domain.model

sealed interface ScreenTarget {
    data object Root : ScreenTarget

    data class Section(val id: String) : ScreenTarget

    data class Page(val id: String) : ScreenTarget
}
