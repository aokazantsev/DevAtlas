package ru.aokazantsev.devatlas.update.domain.model

data class ReleaseApk(
    val url: String,
    val sizeBytes: Long,
    val sha256: String?,
)
