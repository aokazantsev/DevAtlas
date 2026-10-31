package ru.aokazantsev.devatlas.files.domain.model

data class DownloadedFile(
    val url: String,
    val contentUri: String,
    val name: String,
    val sizeBytes: Long,
    val downloadedAtMillis: Long,
)
