package ru.aokazantsev.devatlas.themes.domain.model

data class InstalledTheme(
    val id: String,
    val title: String,
    val description: String?,
    val sourceUrl: String,
    val etag: String?,
    val lastModified: String?,
    val sha256: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val lastUsedAt: Long,
)
