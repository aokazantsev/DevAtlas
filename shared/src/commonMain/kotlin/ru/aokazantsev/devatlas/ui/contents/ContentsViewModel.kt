package ru.aokazantsev.devatlas.ui.contents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary
import ru.aokazantsev.devatlas.themes.domain.ThemePackageReader
import ru.aokazantsev.devatlas.themes.domain.ThemeStorage

@Inject
class ContentsViewModel(
    library: ThemeLibrary,
    private val storage: ThemeStorage,
    private val reader: ThemePackageReader,
) : ViewModel() {

    val state: StateFlow<ContentsState> = library.observeCurrent()
        .map { theme ->
            if (theme == null) {
                ContentsState.NoTheme
            } else {
                val manifest = withContext(Dispatchers.IO) { reader.manifest(storage.themeDirectory(theme.id)) }
                ContentsState.Ready(
                    title = theme.title,
                    description = theme.description,
                    sections = manifest?.tree?.map { it.title }.orEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ContentsState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
