package ru.aokazantsev.devatlas.ui.update

sealed interface UpdateStep {
    data object Idle : UpdateStep

    data class Downloading(val receivedBytes: Long, val totalBytes: Long?) : UpdateStep

    data object NeedsPermission : UpdateStep

    data object Installing : UpdateStep

    data class Failed(val message: String) : UpdateStep
}
