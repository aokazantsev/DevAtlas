package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.ui.text.AnnotatedString
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline

internal class InlineContent(
    val text: AnnotatedString,
    val links: List<PageInline.Link>,
)
