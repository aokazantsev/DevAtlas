package ru.aokazantsev.devatlas.e2e

import ru.aokazantsev.devatlas.di.NetworkEndpoints

object TestNetwork {

    @Volatile
    var baseUrl: String? = null

    fun endpoints(): NetworkEndpoints {
        val base = checkNotNull(baseUrl) { "MockServerRule должен стартовать раньше приложения" }.trimEnd('/')
        return NetworkEndpoints(
            sampleThemeUrl = "$base/pets/devatlas/sample-theme.zip",
            releasesUrl = "$base/repos/aokazantsev/DevAtlas/releases",
        )
    }
}
