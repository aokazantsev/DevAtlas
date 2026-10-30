package ru.aokazantsev.devatlas.ui.themes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary

@Inject
class ThemesViewModel(
    library: ThemeLibrary,
) : ViewModel() {

    val state: StateFlow<ThemesState?> = combine(
        library.observeInstalled(),
        library.observeCurrent().map { it?.id },
    ) { themes, currentId -> ThemesState(themes = themes, currentId = currentId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
