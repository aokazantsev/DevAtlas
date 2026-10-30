package ru.aokazantsev.devatlas.contents.domain.model

import ru.aokazantsev.devatlas.reading.domain.model.Bookmark
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

sealed interface ThemeContents {
    data object NoTheme : ThemeContents

    data object Unreadable : ThemeContents

    data class Ready(
        val theme: InstalledTheme,
        val manifest: ThemeManifest,
        val viewedPageIds: Set<String>,
        val bookmarks: List<Bookmark>,
    ) : ThemeContents
}
