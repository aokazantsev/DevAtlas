package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.InlineStyle
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline

@Composable
internal fun rememberInlineText(content: List<PageInline>, onLink: (LinkTarget) -> Unit): AnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = AtlasTheme.colors.primary, textDecoration = TextDecoration.Underline),
    )
    val codeStyle = SpanStyle(
        fontFamily = AtlasTheme.typography.code.fontFamily,
        background = AtlasTheme.colors.surfaceContainerHighest,
    )
    return remember(content, linkStyles, codeStyle, onLink) {
        buildAnnotatedString { appendInlines(content, InlineSpans(linkStyles, codeStyle, onLink)) }
    }
}

private fun AnnotatedString.Builder.appendInlines(content: List<PageInline>, spans: InlineSpans) {
    content.forEach { inline ->
        when (inline) {
            is PageInline.Text -> withStyle(inline.style.toSpan(spans.codeStyle)) { append(inline.text) }
            PageInline.LineBreak -> append('\n')
            is PageInline.Link -> withLink(
                LinkAnnotation.Clickable(
                    tag = inline.target.toString(),
                    styles = spans.linkStyles,
                    linkInteractionListener = { spans.onLink(inline.target) },
                ),
            ) { appendInlines(inline.content, spans) }
        }
    }
}

private fun InlineStyle.toSpan(codeStyle: SpanStyle): SpanStyle {
    var span = if (code) codeStyle else SpanStyle()
    if (bold) span = span.merge(SpanStyle(fontWeight = FontWeight.Bold))
    if (italic) span = span.merge(SpanStyle(fontStyle = FontStyle.Italic))
    return span
}
