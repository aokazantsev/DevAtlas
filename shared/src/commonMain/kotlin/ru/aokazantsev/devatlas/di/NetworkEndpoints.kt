package ru.aokazantsev.devatlas.di

data class NetworkEndpoints(
    val sampleThemeUrl: String,
    val releasesUrl: String,
) {
    companion object {
        val Production = NetworkEndpoints(
            sampleThemeUrl = "https://aokazantsev.ru/pets/devatlas/sample-theme.zip",
            releasesUrl = "https://api.github.com/repos/aokazantsev/DevAtlas/releases",
        )
    }
}
