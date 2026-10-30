package ru.aokazantsev.devatlas.themeformat.manifest

import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue

data class ManifestParseResult(
    val manifest: ThemeManifest?,
    val issues: List<ThemeIssue>,
)
