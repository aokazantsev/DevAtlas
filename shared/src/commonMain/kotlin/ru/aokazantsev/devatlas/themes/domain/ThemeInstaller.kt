package ru.aokazantsev.devatlas.themes.domain

import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure

interface ThemeInstaller {
    suspend fun prepare(address: String): PrepareResult

    suspend fun install(prepared: PreparedTheme): InstallResult

    fun discard(prepared: PreparedTheme)

    suspend fun redownload(themeId: String): ThemeFailure?
}
