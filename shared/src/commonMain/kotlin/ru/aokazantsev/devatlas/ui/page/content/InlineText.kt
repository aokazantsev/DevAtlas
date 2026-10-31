package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.InlineStyle
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline

@Composable
internal fun InlineText(
    content: List<PageInline>,
    links: PageLinks,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val inline = rememberInlineContent(content, links.onClick)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val longPress = if (inline.links.isEmpty()) {
        Modifier
    } else {
        Modifier.linkLongPress(inline, { layout }, links.onLongPress)
    }
    AtlasText(
        text = inline.text,
        modifier = modifier.then(longPress),
        style = style,
        color = color,
        onTextLayout = { layout = it },
    )
}

@Composable
private fun rememberInlineContent(content: List<PageInline>, onClick: (PageInline.Link) -> Unit): InlineContent {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = AtlasTheme.colors.primary, textDecoration = TextDecoration.Underline),
    )
    val codeStyle = SpanStyle(
        fontFamily = AtlasTheme.typography.code.fontFamily,
        background = AtlasTheme.colors.surfaceContainerHighest,
    )
    return remember(content, linkStyles, codeStyle, onClick) {
        val links = mutableListOf<PageInline.Link>()
        val text = buildAnnotatedString { appendInlines(content, InlineSpans(linkStyles, codeStyle, onClick, links)) }
        InlineContent(text, links)
    }
}

internal fun AnnotatedString.linkIndexAt(offset: Int): Int? =
    getLinkAnnotations(offset, offset + 1).firstOrNull()?.let { (it.item as? LinkAnnotation.Clickable)?.tag?.toIntOrNull() }

private fun AnnotatedString.Builder.appendInlines(content: List<PageInline>, spans: InlineSpans) {
    content.forEach { inline ->
        when (inline) {
            is PageInline.Text -> withStyle(inline.style.toSpan(spans.codeStyle)) { append(inline.text) }
            PageInline.LineBreak -> append('\n')
            is PageInline.Link -> {
                val index = spans.links.size
                spans.links += inline
                withLink(
                    LinkAnnotation.Clickable(
                        tag = index.toString(),
                        styles = spans.linkStyles,
                        linkInteractionListener = { spans.onClick(inline) },
                    ),
                ) { appendInlines(inline.content, spans) }
            }
        }
    }
}

private fun InlineStyle.toSpan(codeStyle: SpanStyle): SpanStyle {
    var span = if (code) codeStyle else SpanStyle()
    if (bold) span = span.merge(SpanStyle(fontWeight = FontWeight.Bold))
    if (italic) span = span.merge(SpanStyle(fontStyle = FontStyle.Italic))
    return span
}
