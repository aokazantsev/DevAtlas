package ru.aokazantsev.devatlas.themeformat.page.model

sealed interface LinkTarget {
    data class Page(val id: String) : LinkTarget

    data class Web(val url: String) : LinkTarget

    data class Email(val uri: String) : LinkTarget

    data class Phone(val uri: String) : LinkTarget
}
