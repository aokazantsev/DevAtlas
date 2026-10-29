package ru.aokazantsev.devatlas.ui.splash

sealed interface SplashState {
    data object Checking : SplashState

    data object Ready : SplashState

    data class Failed(val reason: String) : SplashState
}
