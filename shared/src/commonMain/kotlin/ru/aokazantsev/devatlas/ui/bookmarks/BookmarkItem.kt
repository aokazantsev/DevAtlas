package ru.aokazantsev.devatlas.ui.bookmarks

import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget

data class BookmarkItem(
    val id: Long,
    val title: String,
    val place: String,
    val target: ScreenTarget,
    val stack: List<Route>,
)
