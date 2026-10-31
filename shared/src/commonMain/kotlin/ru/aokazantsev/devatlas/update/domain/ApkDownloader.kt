package ru.aokazantsev.devatlas.update.domain

import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.AppRelease

interface ApkDownloader {
    suspend fun download(release: AppRelease, onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit): ApkDownloadResult
}
