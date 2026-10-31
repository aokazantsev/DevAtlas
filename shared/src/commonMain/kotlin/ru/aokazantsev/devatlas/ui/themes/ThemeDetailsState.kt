package ru.aokazantsev.devatlas.ui.themes

import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

data class ThemeDetailsState(
    val theme: InstalledTheme?,
    val isCurrent: Boolean,
    val applyFailure: String? = null,
)
