package ru.aokazantsev.devatlas.themeformat.page

import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.page.model.PageDocument

data class ParsedPage(
    val document: PageDocument,
    val issues: List<ThemeIssue>,
    val pageReferences: List<Reference>,
    val imageReferences: List<Reference>,
)
