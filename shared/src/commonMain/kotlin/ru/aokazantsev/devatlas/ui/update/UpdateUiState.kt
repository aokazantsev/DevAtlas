package ru.aokazantsev.devatlas.ui.update

import ru.aokazantsev.devatlas.update.domain.model.AppRelease

sealed interface UpdateUiState {
    data object Checking : UpdateUiState

    data class UpToDate(val version: String) : UpdateUiState

    data class Failed(val message: String) : UpdateUiState

    data class Available(
        val release: AppRelease,
        val notes: List<ReleaseNotes>,
        val hasOlderNotes: Boolean,
        val offlineNote: String?,
        val step: UpdateStep = UpdateStep.Idle,
    ) : UpdateUiState
}
