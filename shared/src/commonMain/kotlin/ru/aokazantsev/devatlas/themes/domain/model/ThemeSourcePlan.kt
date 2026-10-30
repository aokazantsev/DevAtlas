package ru.aokazantsev.devatlas.themes.domain.model

data class ThemeSourcePlan(
    val address: String,
    val location: ArchiveLocation,
    val subFolder: String?,
)
