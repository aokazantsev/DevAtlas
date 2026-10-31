package ru.aokazantsev.devatlas.update.domain.model

data class ReleaseList(
    val releases: List<AppRelease>,
    val complete: Boolean,
)
