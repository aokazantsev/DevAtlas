package ru.aokazantsev.devatlas.reading.domain.model

import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

data class Continuation(
    val tree: List<ThemeNode>,
    val point: ReadingPoint,
)
