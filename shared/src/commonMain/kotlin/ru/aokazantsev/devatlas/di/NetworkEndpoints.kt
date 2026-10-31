package ru.aokazantsev.devatlas.di

data class NetworkEndpoints(
    val sampleThemeUrl: String,
    val releasesUrl: String,
    val crashReportUrl: String,
    val usageUrl: String,
    val yandexDownloadApiUrl: String = "https://cloud-api.yandex.net/v1/disk/public/resources/download",
    val githubArchiveUrl: String = "https://codeload.github.com",
    val googleDriveDownloadUrl: String = "https://drive.usercontent.google.com/download",
    val allowCleartext: Boolean = false,
    val webRewrites: Map<String, String> = emptyMap(),
) {
    fun webUrl(url: String): String {
        val prefix = webRewrites.keys.firstOrNull { url.startsWith(it, ignoreCase = true) } ?: return url
        return webRewrites.getValue(prefix) + url.substring(prefix.length)
    }

    companion object {
        val Production = NetworkEndpoints(
            sampleThemeUrl = "https://aokazantsev.ru/pets/devatlas/sample-theme.zip",
            releasesUrl = "https://api.github.com/repos/aokazantsev/DevAtlas/releases",
            crashReportUrl = "https://aokazantsev.ru/pets/devatlas/crash-report",
            usageUrl = "https://aokazantsev.ru/pets/devatlas/usage",
        )
    }
}
