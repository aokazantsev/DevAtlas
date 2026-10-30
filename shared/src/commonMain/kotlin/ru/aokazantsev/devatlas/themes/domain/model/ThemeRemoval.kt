package ru.aokazantsev.devatlas.themes.domain.model

sealed interface ThemeRemoval {
    data object ProgressReset : ThemeRemoval

    data object ThemeDeleted : ThemeRemoval

    data class SwitchedTo(val themeId: String) : ThemeRemoval

    data object NoThemesLeft : ThemeRemoval

    data class Failed(val failure: ThemeFailure) : ThemeRemoval
}
