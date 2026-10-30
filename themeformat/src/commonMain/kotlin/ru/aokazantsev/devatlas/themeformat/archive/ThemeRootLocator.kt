package ru.aokazantsev.devatlas.themeformat.archive

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

object ThemeRootLocator {

    fun locate(files: Collection<String>): String? {
        if (ThemeManifest.FILE_NAME in files) return ""
        val topFolders = files.map { it.substringBefore('/', missingDelimiterValue = "") }.toSet()
        val folder = topFolders.singleOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return "$folder/".takeIf { "$it${ThemeManifest.FILE_NAME}" in files }
    }
}
