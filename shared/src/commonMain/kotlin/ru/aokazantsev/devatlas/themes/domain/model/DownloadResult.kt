package ru.aokazantsev.devatlas.themes.domain.model

sealed interface DownloadResult {
    data class Downloaded(
        val etag: String?,
        val lastModified: String?,
        val sizeBytes: Long,
    ) : DownloadResult

    data object NotModified : DownloadResult

    data class Failed(val failure: ThemeFailure) : DownloadResult
}
