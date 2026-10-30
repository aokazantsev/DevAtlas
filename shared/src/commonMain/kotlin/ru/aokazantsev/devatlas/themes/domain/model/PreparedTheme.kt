package ru.aokazantsev.devatlas.themes.domain.model

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

data class PreparedTheme(
    val staging: StagingArea,
    val manifest: ThemeManifest,
    val source: String,
    val etag: String?,
    val lastModified: String?,
    val sha256: String,
    val sizeBytes: Long,
)
