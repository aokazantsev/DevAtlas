package ru.aokazantsev.devatlas.ui.splash

import ru.aokazantsev.devatlas.themes.domain.model.DamagedTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure

sealed interface SplashState {
    data object Checking : SplashState

    data object Ready : SplashState

    data class Failed(val reason: String) : SplashState

    data class ThemeDamaged(
        val theme: DamagedTheme,
        val busyText: String? = null,
        val failure: ThemeFailure? = null,
    ) : SplashState
}
