package ru.aokazantsev.devatlas.themes.domain.model

sealed interface PrepareResult {
    data class Ready(val prepared: PreparedTheme) : PrepareResult

    data class Failed(val failure: ThemeFailure) : PrepareResult
}
