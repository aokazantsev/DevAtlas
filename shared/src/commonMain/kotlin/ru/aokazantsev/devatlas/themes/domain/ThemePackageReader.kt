package ru.aokazantsev.devatlas.themes.domain

import okio.Path
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

interface ThemePackageReader {
    fun manifest(themeDirectory: Path): ThemeManifest?

    fun pageHashes(themeDirectory: Path, manifest: ThemeManifest): Map<String, String>

    fun contentDigest(themeDirectory: Path): String
}
