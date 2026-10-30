package ru.aokazantsev.devatlas.themes.domain

import ru.aokazantsev.devatlas.themes.domain.model.UpdateCheck

interface ThemeUpdater {
    suspend fun check(themeId: String, newAddress: String? = null): UpdateCheck
}
