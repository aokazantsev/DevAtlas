package ru.aokazantsev.devatlas.ui.bookmarks

object BookmarkNames {
    const val MAX_LENGTH = 100

    fun normalize(name: String, fallback: String): String =
        name.trim().take(MAX_LENGTH).ifEmpty { fallback.take(MAX_LENGTH) }
}
