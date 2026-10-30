package ru.aokazantsev.devatlas.themes.domain.model

sealed interface InstallResult {
    data class Installed(val theme: InstalledTheme) : InstallResult

    data class Failed(val failure: ThemeFailure) : InstallResult
}
