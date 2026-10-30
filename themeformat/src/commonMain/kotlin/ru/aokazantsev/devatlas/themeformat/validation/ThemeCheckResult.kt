package ru.aokazantsev.devatlas.themeformat.validation

import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

data class ThemeCheckResult(
    val manifest: ThemeManifest?,
    val issues: List<ThemeIssue>,
) {
    val errors: List<ThemeIssue>
        get() = issues.filter { it.severity == IssueSeverity.ERROR }

    val warnings: List<ThemeIssue>
        get() = issues.filter { it.severity == IssueSeverity.WARNING }

    val passed: Boolean
        get() = manifest != null && errors.isEmpty()
}
