package ru.aokazantsev.devatlas.ui.page

import ru.aokazantsev.devatlas.contents.domain.model.PageContent

sealed interface PageState {
    data object Loading : PageState

    data object Missing : PageState

    data class Unreadable(val title: String?) : PageState

    data class Ready(
        val themeId: String,
        val title: String,
        val content: PageContent,
        val viewed: Boolean,
        val bookmarked: Boolean,
        val next: NextPage?,
    ) : PageState
}
