package ru.aokazantsev.devatlas.themes.domain.model

sealed interface UpdateCheck {
    data object NoChanges : UpdateCheck

    data class Available(val prepared: PreparedTheme, val summary: UpdateSummary, val contentChanged: Boolean) : UpdateCheck

    data class Failed(val failure: ThemeFailure) : UpdateCheck
}
