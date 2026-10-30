package ru.aokazantsev.devatlas.themes.domain.model

sealed interface ArchiveLocation {
    data class Direct(val url: String) : ArchiveLocation

    data class YandexPublic(val publicKey: String, val path: String?) : ArchiveLocation
}
