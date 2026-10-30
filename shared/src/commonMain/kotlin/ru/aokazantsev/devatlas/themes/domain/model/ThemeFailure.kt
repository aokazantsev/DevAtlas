package ru.aokazantsev.devatlas.themes.domain.model

import ru.aokazantsev.devatlas.text.russianPlural
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue

data class ThemeFailure(
    val message: String,
    val helpUrl: String? = null,
) {
    companion object {
        fun fromIssues(errors: List<ThemeIssue>): ThemeFailure {
            val first = errors.first()
            val place = first.location.takeIf { it.isNotEmpty() }?.let { "$it: " }.orEmpty()
            val more = errors.size - 1
            val tail = if (more > 0) "\nИ ещё $more ${russianPlural(more, one = "ошибка", few = "ошибки", many = "ошибок")}." else ""
            return ThemeFailure(message = place + first.message + tail, helpUrl = first.helpUrl)
        }
    }
}
