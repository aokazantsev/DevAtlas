package ru.aokazantsev.devatlas.ui.downloads

sealed interface DownloadsState {
    data object Loading : DownloadsState

    data class Ready(val rows: List<DownloadRow>) : DownloadsState
}
