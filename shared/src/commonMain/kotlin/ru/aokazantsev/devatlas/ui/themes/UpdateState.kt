package ru.aokazantsev.devatlas.ui.themes

import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.UpdateSummary

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object Applying : UpdateState

    data class Message(val text: String) : UpdateState

    data class Failed(val failure: ThemeFailure) : UpdateState

    data class Available(
        val prepared: PreparedTheme,
        val summary: UpdateSummary,
        val sourceChanged: Boolean,
        val contentChanged: Boolean,
    ) : UpdateState
}
