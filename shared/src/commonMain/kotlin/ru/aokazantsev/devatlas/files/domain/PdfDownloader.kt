package ru.aokazantsev.devatlas.files.domain

import ru.aokazantsev.devatlas.files.domain.model.PdfDownloadResult
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest

interface PdfDownloader {
    suspend fun download(
        request: PdfRequest,
        replacing: String?,
        onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): PdfDownloadResult
}
