package ru.aokazantsev.devatlas.themes.domain.model

sealed interface SourceResolution {
    data class Resolved(val plan: ThemeSourcePlan) : SourceResolution

    data class Rejected(val message: String) : SourceResolution
}
