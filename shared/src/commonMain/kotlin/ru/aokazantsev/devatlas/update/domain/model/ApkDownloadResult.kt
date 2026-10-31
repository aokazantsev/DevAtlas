package ru.aokazantsev.devatlas.update.domain.model

import okio.Path

sealed interface ApkDownloadResult {
    data class Saved(val file: Path) : ApkDownloadResult

    data class Failed(val message: String, val retryable: Boolean = true) : ApkDownloadResult
}
