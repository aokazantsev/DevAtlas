package ru.aokazantsev.devatlas.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent

@Inject
class OnboardingViewModel(
    private val onboarding: OnboardingRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        OnboardingState(
            policyAccepted = false,
            crashReports = Consent.Default.crashReports,
            usageStatistics = Consent.Default.usageStatistics,
        ),
    )
    val state: StateFlow<OnboardingState> = mutableState.asStateFlow()

    fun setPolicyAccepted(accepted: Boolean) {
        mutableState.update { it.copy(policyAccepted = accepted, failure = null) }
    }

    fun setCrashReports(enabled: Boolean) {
        mutableState.update { it.copy(crashReports = enabled) }
    }

    fun setUsageStatistics(enabled: Boolean) {
        mutableState.update { it.copy(usageStatistics = enabled) }
    }

    fun next(onDone: () -> Unit) {
        val current = mutableState.value
        if (!current.policyAccepted) return
        viewModelScope.launch {
            if (onboarding.accept(crashReports = current.crashReports, usageStatistics = current.usageStatistics)) {
                mutableState.update { it.copy(failure = null) }
                onDone()
            } else {
                mutableState.update { it.copy(failure = SAVE_FAILED) }
            }
        }
    }

    private companion object {
        const val SAVE_FAILED = "Не удалось сохранить выбор на телефон: проверьте свободное место и нажмите «Дальше» ещё раз"
    }
}
