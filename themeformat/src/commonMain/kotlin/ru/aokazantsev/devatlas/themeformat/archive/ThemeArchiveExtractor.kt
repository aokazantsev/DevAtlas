package ru.aokazantsev.devatlas.themeformat.archive

import okio.FileSystem
import okio.Path

interface ThemeArchiveExtractor {
    fun extract(fileSystem: FileSystem, archive: Path, target: Path, subFolder: String? = null): ExtractionResult
}
