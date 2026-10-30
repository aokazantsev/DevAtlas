package ru.aokazantsev.devatlas.themes.domain.model

sealed interface ArchiveLocation {
    data class Direct(val url: String) : ArchiveLocation

    data class YandexPublic(val publicKey: String, val path: String?) : ArchiveLocation

    data class GoogleDriveFile(val fileId: String, val resourceKey: String? = null) : ArchiveLocation
}
