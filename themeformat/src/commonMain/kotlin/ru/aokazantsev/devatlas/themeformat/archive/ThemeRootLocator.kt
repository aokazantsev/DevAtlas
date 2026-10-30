package ru.aokazantsev.devatlas.themeformat.archive

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

object ThemeRootLocator {

    fun locate(files: Collection<String>, subFolder: String? = null): String? {
        val inner = subFolder?.trim('/')?.takeIf { it.isNotEmpty() }?.let { "$it/" }.orEmpty()
        val flat = inner.takeIf { subFolder == null }
        if (flat != null && ThemeManifest.FILE_NAME in files) return flat
        val topFolders = files.map { it.substringBefore('/', missingDelimiterValue = "") }.toSet()
        val folder = topFolders.singleOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return "$folder/$inner".takeIf { "$it${ThemeManifest.FILE_NAME}" in files }
    }
}
