package ru.aokazantsev.devatlas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.settings.domain.ReadingSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode

@Inject
class SettingsViewModel(
    private val themeSettings: ThemeSettingsRepository,
    private val readingSettings: ReadingSettingsRepository,
) : ViewModel() {

    val appearance: StateFlow<Appearance?> = themeSettings.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val continueFromLastPoint: StateFlow<Boolean?> = readingSettings.continueFromLastPoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setContinueFromLastPoint(enabled: Boolean) {
        viewModelScope.launch { readingSettings.setContinueFromLastPoint(enabled) }
    }

    fun selectThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeSettings.setThemeMode(mode) }
    }

    fun setWallpaperColors(enabled: Boolean) {
        viewModelScope.launch { themeSettings.setWallpaperColors(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
