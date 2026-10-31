package ru.aokazantsev.devatlas.startup.domain

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

interface VerifiedManifests {
    fun replace(manifests: Map<String, ThemeManifest>)

    fun find(themeId: String): ThemeManifest?
}
