package ru.aokazantsev.devatlas.update.domain.model

sealed interface ReleasesResult {
    data class Loaded(val list: ReleaseList) : ReleasesResult

    data class Failed(val message: String, val cached: ReleaseList?) : ReleasesResult
}
