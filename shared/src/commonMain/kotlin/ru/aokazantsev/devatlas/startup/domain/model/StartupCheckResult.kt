package ru.aokazantsev.devatlas.startup.domain.model

sealed interface StartupCheckResult {
    data object Passed : StartupCheckResult

    data class Failed(val reason: String) : StartupCheckResult
}
