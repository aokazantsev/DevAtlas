package ru.aokazantsev.devatlas.ui.update

import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock

data class ReleaseNotes(
    val version: String,
    val blocks: List<PageBlock>,
)
