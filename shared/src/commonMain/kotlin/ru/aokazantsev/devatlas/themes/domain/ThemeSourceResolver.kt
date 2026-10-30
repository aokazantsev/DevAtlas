package ru.aokazantsev.devatlas.themes.domain

import ru.aokazantsev.devatlas.themes.domain.model.SourceResolution

interface ThemeSourceResolver {
    fun resolve(input: String): SourceResolution
}
