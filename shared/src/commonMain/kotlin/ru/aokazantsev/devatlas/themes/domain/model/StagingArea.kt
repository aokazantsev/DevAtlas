package ru.aokazantsev.devatlas.themes.domain.model

import okio.Path

data class StagingArea(
    val directory: Path,
    val archive: Path,
    val content: Path,
)
