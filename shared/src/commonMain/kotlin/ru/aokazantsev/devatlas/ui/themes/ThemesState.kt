package ru.aokazantsev.devatlas.ui.themes

import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

data class ThemesState(
    val themes: List<InstalledTheme>,
    val currentId: String?,
)
