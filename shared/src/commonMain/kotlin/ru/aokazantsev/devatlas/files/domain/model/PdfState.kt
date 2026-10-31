package ru.aokazantsev.devatlas.files.domain.model

sealed interface PdfState {
    data object Idle : PdfState

    data class Downloading(val receivedBytes: Long, val totalBytes: Long?) : PdfState

    data class Failed(val message: String, val request: PdfRequest, val redownload: Boolean) : PdfState

    data class NoViewer(val fileName: String) : PdfState

    data class Ready(val file: DownloadedFile) : PdfState
}
