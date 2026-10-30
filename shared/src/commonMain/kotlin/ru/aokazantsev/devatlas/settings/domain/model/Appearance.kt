package ru.aokazantsev.devatlas.settings.domain.model

data class Appearance(
    val themeMode: ThemeMode,
    val wallpaperColors: Boolean,
) {
    companion object {
        val Default = Appearance(themeMode = ThemeMode.SYSTEM, wallpaperColors = true)
    }
}
