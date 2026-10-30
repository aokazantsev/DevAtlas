package ru.aokazantsev.devatlas.ui.contents

sealed interface ContentsRow {
    val id: String
    val title: String

    data class Page(override val id: String, override val title: String, val viewed: Boolean) : ContentsRow

    data class Section(override val id: String, override val title: String, val progress: ContentsProgress) : ContentsRow
}
