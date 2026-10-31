package ru.aokazantsev.devatlas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.settings.domain.ReadingSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.update.domain.UpdateSettingsRepository
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@Inject
class SettingsViewModel(
    private val themeSettings: ThemeSettingsRepository,
    private val readingSettings: ReadingSettingsRepository,
    private val onboarding: OnboardingRepository,
    private val updateSettings: UpdateSettingsRepository,
    private val usage: UsageStatistics,
) : ViewModel() {

    val appearance: StateFlow<Appearance?> = themeSettings.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val continueFromLastPoint: StateFlow<Boolean?> = readingSettings.continueFromLastPoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val consent: StateFlow<Consent?> = onboarding.consent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val autoUpdate: StateFlow<Boolean?> = updateSettings.autoCheck
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setContinueFromLastPoint(enabled: Boolean) = save { readingSettings.setContinueFromLastPoint(enabled) }

    fun selectThemeMode(mode: ThemeMode) = save { themeSettings.setThemeMode(mode) }

    fun setWallpaperColors(enabled: Boolean) = save { themeSettings.setWallpaperColors(enabled) }

    fun setAutoUpdate(enabled: Boolean) = save { updateSettings.setAutoCheck(enabled) }

    fun setCrashReports(enabled: Boolean) = save { onboarding.setCrashReports(enabled) }

    fun setUsageStatistics(enabled: Boolean) {
        viewModelScope.launch {
            onboarding.setUsageStatistics(enabled)
            if (enabled) usage.hit(UsageEvent.SETTINGS_SAVE) else usage.forget()
        }
    }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            write()
            usage.hit(UsageEvent.SETTINGS_SAVE)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
