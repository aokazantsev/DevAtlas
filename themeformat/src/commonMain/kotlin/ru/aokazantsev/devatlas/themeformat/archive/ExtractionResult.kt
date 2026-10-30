package ru.aokazantsev.devatlas.themeformat.archive

import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue

data class ExtractionResult(
    val files: List<String>,
    val issues: List<ThemeIssue>,
) {
    val succeeded: Boolean
        get() = issues.isEmpty()
}
