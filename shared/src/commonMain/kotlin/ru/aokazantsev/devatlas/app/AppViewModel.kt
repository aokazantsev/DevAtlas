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
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.startup.domain.RunStartupChecksUseCase
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import ru.aokazantsev.devatlas.ui.splash.SplashState

@Inject
class AppViewModel(
    private val runStartupChecks: RunStartupChecksUseCase,
    themeSettings: ThemeSettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = themeSettings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableStartupState = MutableStateFlow<SplashState>(SplashState.Checking)
    val startupState: StateFlow<SplashState> = mutableStartupState.asStateFlow()

    init {
        check()
    }

    fun retry() {
        mutableStartupState.value = SplashState.Checking
        check()
    }

    private fun check() {
        viewModelScope.launch {
            mutableStartupState.value = when (val result = runStartupChecks()) {
                StartupCheckResult.Passed -> SplashState.Ready
                is StartupCheckResult.Failed -> SplashState.Failed(result.reason)
            }
        }
    }
}
