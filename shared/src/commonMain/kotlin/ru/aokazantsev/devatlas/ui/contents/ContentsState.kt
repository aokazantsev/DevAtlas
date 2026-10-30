package ru.aokazantsev.devatlas.ui.contents

sealed interface ContentsState {
    data object Loading : ContentsState

    data object NoTheme : ContentsState

    data class Ready(val title: String, val description: String?, val sections: List<String>) : ContentsState
}
