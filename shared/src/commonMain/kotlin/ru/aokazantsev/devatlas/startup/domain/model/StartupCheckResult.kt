package ru.aokazantsev.devatlas.startup.domain.model

import ru.aokazantsev.devatlas.themes.domain.model.DamagedTheme

sealed interface StartupCheckResult {
    data object Passed : StartupCheckResult

    data class Failed(val reason: String) : StartupCheckResult

    data class ThemeDamaged(val theme: DamagedTheme) : StartupCheckResult
}
