package ru.aokazantsev.devatlas.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.startup.domain.RunStartupChecksUseCase
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import ru.aokazantsev.devatlas.ui.splash.SplashState

@Inject
class AppViewModel(
    private val runStartupChecks: RunStartupChecksUseCase,
    private val onboarding: OnboardingRepository,
    private val library: ThemeLibrary,
    private val installer: ThemeInstaller,
    themeSettings: ThemeSettingsRepository,
) : ViewModel() {

    val appearance: StateFlow<Appearance?> = themeSettings.appearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableStartupState = MutableStateFlow<SplashState>(SplashState.Checking)
    val startupState: StateFlow<SplashState> = mutableStartupState.asStateFlow()

    private val mutableStartRoute = MutableStateFlow<Route?>(null)
    val startRoute: StateFlow<Route?> = mutableStartRoute.asStateFlow()

    init {
        check()
    }

    fun retry() {
        mutableStartupState.value = SplashState.Checking
        check()
    }

    fun redownloadDamagedTheme() {
        val damaged = mutableStartupState.value as? SplashState.ThemeDamaged ?: return
        if (damaged.busyText != null) return
        mutableStartupState.value = damaged.copy(busyText = "Скачиваю тему заново…", failure = null)
        viewModelScope.launch {
            val failure = installer.redownload(damaged.theme.themeId)
            if (failure == null) {
                check()
            } else {
                mutableStartupState.value = damaged.copy(busyText = null, failure = failure)
            }
        }
    }

    fun chooseAnotherTheme() {
        val damaged = mutableStartupState.value as? SplashState.ThemeDamaged ?: return
        if (damaged.busyText != null) return
        mutableStartupState.value = damaged.copy(busyText = "Убираю тему…", failure = null)
        viewModelScope.launch {
            val removal = library.remove(damaged.theme.themeId, deleteTheme = true, resetProgress = false)
            if (removal is ThemeRemoval.Failed) {
                mutableStartupState.value = damaged.copy(busyText = null, failure = removal.failure)
            } else {
                check()
            }
        }
    }

    private fun check() {
        viewModelScope.launch {
            mutableStartupState.value = when (val result = runStartupChecks()) {
                StartupCheckResult.Passed -> {
                    mutableStartRoute.value = resolveStartRoute()
                    SplashState.Ready
                }
                is StartupCheckResult.Failed -> SplashState.Failed(result.reason)
                is StartupCheckResult.ThemeDamaged -> SplashState.ThemeDamaged(result.theme)
            }
        }
    }

    private suspend fun resolveStartRoute(): Route = when {
        !onboarding.isPolicyAccepted() -> Route.Onboarding
        library.resolveCurrent() != null -> Route.Contents
        else -> Route.AddTheme(onboarding = true)
    }
}
