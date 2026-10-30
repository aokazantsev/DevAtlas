package ru.aokazantsev.devatlas.ui.bookmarks

sealed interface BookmarksState {
    data object Loading : BookmarksState

    data object NoTheme : BookmarksState

    data class Ready(val items: List<BookmarkItem>) : BookmarksState
}
