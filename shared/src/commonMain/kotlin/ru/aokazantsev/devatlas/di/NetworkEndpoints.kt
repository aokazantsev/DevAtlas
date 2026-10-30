package ru.aokazantsev.devatlas.di

data class NetworkEndpoints(
    val sampleThemeUrl: String,
    val releasesUrl: String,
    val yandexDownloadApiUrl: String = "https://cloud-api.yandex.net/v1/disk/public/resources/download",
    val githubArchiveUrl: String = "https://codeload.github.com",
    val googleDriveDownloadUrl: String = "https://drive.usercontent.google.com/download",
    val allowCleartext: Boolean = false,
) {
    companion object {
        val Production = NetworkEndpoints(
            sampleThemeUrl = "https://aokazantsev.ru/pets/devatlas/sample-theme.zip",
            releasesUrl = "https://api.github.com/repos/aokazantsev/DevAtlas/releases",
        )
    }
}
