package ru.aokazantsev.devatlas.themeformat.manifest

object ThemeIds {

    const val MAX_LENGTH = 64

    private val pattern = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    fun isValid(id: String): Boolean = id.length <= MAX_LENGTH && pattern.matches(id)
}
