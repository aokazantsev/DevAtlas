package ru.aokazantsev.devatlas.ui.themes

data class RemovalConfirm(
    val themeTitle: String,
    val deleteTheme: Boolean,
    val resetProgress: Boolean,
    val viewedCount: Int,
    val bookmarkCount: Int,
    val isCurrent: Boolean,
)
