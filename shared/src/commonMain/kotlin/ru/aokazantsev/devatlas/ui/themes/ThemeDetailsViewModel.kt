package ru.aokazantsev.devatlas.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary
import ru.aokazantsev.devatlas.themes.domain.ThemeUpdater
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@AssistedInject
class ThemeDetailsViewModel(
    @Assisted private val themeId: String,
    private val library: ThemeLibrary,
    updater: ThemeUpdater,
    installer: ThemeInstaller,
    private val usage: UsageStatistics,
) : ViewModel() {

    val update = ThemeUpdateController(themeId, updater, installer, viewModelScope)

    val state: StateFlow<ThemeDetailsState?> = combine(
        library.observeInstalled().map { themes -> themes.firstOrNull { it.id == themeId } },
        library.observeCurrent().map { it?.id == themeId },
    ) { theme, isCurrent -> ThemeDetailsState(theme = theme, isCurrent = isCurrent) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun apply(onApplied: () -> Unit) {
        viewModelScope.launch {
            library.apply(themeId)
            usage.hit(UsageEvent.THEME_SWITCH)
            onApplied()
        }
    }

    override fun onCleared() {
        update.release()
        super.onCleared()
    }

    @AssistedFactory
    fun interface Factory {
        fun create(themeId: String): ThemeDetailsViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
