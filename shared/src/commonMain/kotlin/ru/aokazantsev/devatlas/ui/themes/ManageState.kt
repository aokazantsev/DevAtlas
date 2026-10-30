package ru.aokazantsev.devatlas.ui.themes

import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

data class ManageState(
    val loaded: Boolean = false,
    val theme: InstalledTheme? = null,
    val sourceAddress: String = "",
    val deleteTheme: Boolean = false,
    val resetProgress: Boolean = false,
    val confirm: RemovalConfirm? = null,
    val busy: Boolean = false,
    val notice: String? = null,
) {
    val canContinue: Boolean
        get() = deleteTheme || resetProgress
}
