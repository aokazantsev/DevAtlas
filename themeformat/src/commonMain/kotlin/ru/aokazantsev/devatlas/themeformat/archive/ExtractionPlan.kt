package ru.aokazantsev.devatlas.themeformat.archive

internal data class ExtractionPlan(
    val root: String,
    val entries: List<ZipEntryRecord>,
)
