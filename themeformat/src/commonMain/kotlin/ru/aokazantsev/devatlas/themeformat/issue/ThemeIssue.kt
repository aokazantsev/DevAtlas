package ru.aokazantsev.devatlas.themeformat.issue

data class ThemeIssue(
    val severity: IssueSeverity,
    val code: IssueCode,
    val message: String,
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
) {
    val helpUrl: String
        get() = code.section.url

    val location: String
        get() = buildString {
            if (file != null) append(file)
            if (line != null) append(":").append(line)
            if (line != null && column != null) append(":").append(column)
        }
}
