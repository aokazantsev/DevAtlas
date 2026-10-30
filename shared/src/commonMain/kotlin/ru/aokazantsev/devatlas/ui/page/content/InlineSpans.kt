package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget

internal class InlineSpans(
    val linkStyles: TextLinkStyles,
    val codeStyle: SpanStyle,
    val onLink: (LinkTarget) -> Unit,
)
