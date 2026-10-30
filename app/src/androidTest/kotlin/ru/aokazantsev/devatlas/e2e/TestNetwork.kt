package ru.aokazantsev.devatlas.e2e

import ru.aokazantsev.devatlas.di.NetworkEndpoints

object TestNetwork {

    const val SAMPLE_PATH = "/pets/devatlas/sample-theme.zip"
    const val GITHUB_PATH = "/codeload"

    @Volatile
    var baseUrl: String? = null

    fun endpoints(): NetworkEndpoints {
        val base = checkNotNull(baseUrl) { "MockServerRule должен стартовать раньше приложения" }.trimEnd('/')
        return NetworkEndpoints(
            sampleThemeUrl = "$base$SAMPLE_PATH",
            releasesUrl = "$base/repos/aokazantsev/DevAtlas/releases",
            yandexDownloadApiUrl = "$base${MockServerRule.YANDEX_API_PATH}",
            githubArchiveUrl = "$base$GITHUB_PATH",
            allowCleartext = true,
        )
    }
}
