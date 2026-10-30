package ru.aokazantsev.devatlas.themeformat.issue

import ru.aokazantsev.devatlas.themeformat.markup.SourcePosition

internal class IssueCollector(private val file: String?) {

    private val collected = mutableListOf<ThemeIssue>()
    private var suppressed = 0
    private var suppressedSeverity = IssueSeverity.WARNING

    val issues: List<ThemeIssue>
        get() = if (suppressed == 0) collected else collected + summary()

    fun error(code: IssueCode, message: String, position: SourcePosition? = null) {
        add(IssueSeverity.ERROR, code, message, position)
    }

    fun warning(code: IssueCode, message: String, position: SourcePosition? = null) {
        add(IssueSeverity.WARNING, code, message, position)
    }

    private fun add(severity: IssueSeverity, code: IssueCode, message: String, position: SourcePosition?) {
        keep(
            ThemeIssue(
                severity = severity,
                code = code,
                message = message,
                file = file,
                line = position?.line,
                column = position?.column,
            ),
        )
    }

    private fun keep(issue: ThemeIssue) {
        if (collected.size < LIMIT) {
            collected += issue
            return
        }
        suppressed++
        if (issue.severity == IssueSeverity.ERROR) suppressedSeverity = IssueSeverity.ERROR
    }

    private fun summary() = ThemeIssue(
        severity = suppressedSeverity,
        code = collected.last().code,
        message = "И ещё замечаний в этом файле: $suppressed",
        file = file,
    )

    private companion object {
        const val LIMIT = 100
    }
}
