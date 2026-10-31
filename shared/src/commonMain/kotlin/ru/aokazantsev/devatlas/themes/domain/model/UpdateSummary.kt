package ru.aokazantsev.devatlas.themes.domain.model

data class UpdateSummary(
    val added: Int,
    val removed: Int,
    val changed: Int,
    val removedSections: Int = 0,
) {
    val isEmpty: Boolean
        get() = added == 0 && removed == 0 && changed == 0 && removedSections == 0
}
