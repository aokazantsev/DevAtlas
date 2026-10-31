package ru.aokazantsev.devatlas.files.domain.model

sealed interface PdfDownloadResult {
    data class Saved(val file: DownloadedFile) : PdfDownloadResult

    data class Failed(val message: String) : PdfDownloadResult
}
