package ru.aokazantsev.devatlas.themes.domain.model

sealed interface FetchResult {
    data class Fetched(val prepared: PreparedTheme) : FetchResult

    data object NotModified : FetchResult

    data class Failed(val failure: ThemeFailure) : FetchResult
}
