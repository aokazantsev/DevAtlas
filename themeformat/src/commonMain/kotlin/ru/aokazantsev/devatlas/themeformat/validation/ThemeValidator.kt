package ru.aokazantsev.devatlas.themeformat.validation

import okio.FileSystem
import okio.Path

interface ThemeValidator {
    fun validate(fileSystem: FileSystem, root: Path): ThemeCheckResult
}
