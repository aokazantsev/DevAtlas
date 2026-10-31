package ru.aokazantsev.devatlas.e2e

import ru.aokazantsev.devatlas.di.NetworkEndpoints

object TestNetwork {

    const val SAMPLE_PATH = "/pets/devatlas/sample-theme.zip"
    const val GITHUB_PATH = "/codeload"
    const val WEB_ORIGIN = "https://devatlas.test"
    const val OFFLINE_ORIGIN = "https://offline.test"
    private const val REFUSED_ORIGIN = "http://127.0.0.1:1"

    @Volatile
    var baseUrl: String? = null

    fun endpoints(): NetworkEndpoints {
        val base = checkNotNull(baseUrl) { "MockServerRule должен стартовать раньше приложения" }.trimEnd('/')
        return NetworkEndpoints(
            sampleThemeUrl = "$base$SAMPLE_PATH",
            releasesUrl = "$base/repos/aokazantsev/DevAtlas/releases",
            yandexDownloadApiUrl = "$base${MockServerRule.YANDEX_API_PATH}",
            githubArchiveUrl = "$base$GITHUB_PATH",
            googleDriveDownloadUrl = "$base${MockServerRule.GOOGLE_DRIVE_PATH}",
            allowCleartext = true,
            webRewrites = mapOf(WEB_ORIGIN to base, OFFLINE_ORIGIN to REFUSED_ORIGIN),
        )
    }
}
