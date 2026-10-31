package ru.aokazantsev.devatlas.startup.data

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

internal sealed interface ThemeVerdict {
    data class Healthy(val manifest: ThemeManifest) : ThemeVerdict

    data class Damaged(val reason: String) : ThemeVerdict
}
