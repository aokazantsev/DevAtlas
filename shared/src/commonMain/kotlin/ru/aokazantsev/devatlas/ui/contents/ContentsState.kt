package ru.aokazantsev.devatlas.ui.contents

sealed interface ContentsState {
    data object Loading : ContentsState

    data object NoTheme : ContentsState

    data object Unreadable : ContentsState

    data object SectionMissing : ContentsState

    data class Ready(
        val themeId: String,
        val title: String,
        val description: String?,
        val progress: ContentsProgress?,
        val rows: List<ContentsRow>,
        val bookmarked: Boolean,
    ) : ContentsState
}
