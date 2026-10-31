package ru.aokazantsev.devatlas.ui.web

internal object WebHost {
    private val AUTHORITY_END = charArrayOf('/', '\\', '?', '#')

    fun of(url: String): String {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        val end = afterScheme.indexOfAny(AUTHORITY_END)
        val authority = if (end < 0) afterScheme else afterScheme.substring(0, end)
        return authority.substringAfterLast('@')
    }
}
