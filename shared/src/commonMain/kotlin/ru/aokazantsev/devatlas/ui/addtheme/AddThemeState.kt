package ru.aokazantsev.devatlas.ui.addtheme

import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure

sealed interface AddThemeState {
    data class Editing(val address: String, val hint: String?) : AddThemeState

    data class Loading(val message: String) : AddThemeState

    data class Preview(val prepared: PreparedTheme, val existing: InstalledTheme?, val confirmReplace: Boolean) : AddThemeState

    data class Failed(val address: String, val failure: ThemeFailure) : AddThemeState
}
