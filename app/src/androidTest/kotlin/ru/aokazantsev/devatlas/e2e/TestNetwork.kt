package ru.aokazantsev.devatlas.e2e

import ru.aokazantsev.devatlas.di.NetworkEndpoints

object TestNetwork {

    const val SAMPLE_PATH = "/pets/devatlas/sample-theme.zip"
    const val GITHUB_PATH = "/codeload"
    const val RELEASES_PATH = "/repos/aokazantsev/DevAtlas/releases"
    const val CRASH_REPORT_PATH = "/pets/devatlas/crash-report"
    const val USAGE_PATH = "/pets/devatlas/usage"
    const val WEB_ORIGIN = "https://devatlas.test"
    const val SITE_ORIGIN = "https://aokazantsev.ru"
    const val OFFLINE_ORIGIN = "https://offline.test"
    const val LATE_ORIGIN = "https://late.test"
    const val LONG_HOST = "www.gosuslugi.ru.login-check.account-verify.secure-session.personal-cabinet.confirm-identity.example.top"
    const val LONG_HOST_ORIGIN = "https://$LONG_HOST"
    private const val REFUSED_ORIGIN = "http://127.0.0.1:1"

    @Volatile
    var baseUrl: String? = null

    @Volatile
    var lateSitePort: Int? = null

    fun endpoints(): NetworkEndpoints {
        val base = checkNotNull(baseUrl) { "MockServerRule должен стартовать раньше приложения" }.trimEnd('/')
        return NetworkEndpoints(
            sampleThemeUrl = "$base$SAMPLE_PATH",
            releasesUrl = "$base$RELEASES_PATH",
            crashReportUrl = "$base$CRASH_REPORT_PATH",
            usageUrl = "$base$USAGE_PATH",
            yandexDownloadApiUrl = "$base${MockServerRule.YANDEX_API_PATH}",
            githubArchiveUrl = "$base$GITHUB_PATH",
            googleDriveDownloadUrl = "$base${MockServerRule.GOOGLE_DRIVE_PATH}",
            allowCleartext = true,
            webRewrites = mapOf(
                WEB_ORIGIN to base,
                SITE_ORIGIN to base,
                LONG_HOST_ORIGIN to base,
                OFFLINE_ORIGIN to REFUSED_ORIGIN,
                LATE_ORIGIN to "http://127.0.0.1:${checkNotNull(lateSitePort)}",
            ),
        )
    }
}
