package ru.aokazantsev.devatlas.ui.page.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

@Immutable
data class CodePalette(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val type: Color,
    val comment: Color,
) {
    fun styleOf(kind: CodeTokenKind): SpanStyle = when (kind) {
        CodeTokenKind.KEYWORD, CodeTokenKind.TAG -> SpanStyle(color = keyword)
        CodeTokenKind.HEADING -> SpanStyle(color = keyword, fontWeight = FontWeight.SemiBold)
        CodeTokenKind.STRING, CodeTokenKind.INSERTED -> SpanStyle(color = string)
        CodeTokenKind.NUMBER, CodeTokenKind.DELETED -> SpanStyle(color = number)
        CodeTokenKind.TYPE,
        CodeTokenKind.META,
        CodeTokenKind.ATTRIBUTE,
        CodeTokenKind.PROPERTY,
        CodeTokenKind.VARIABLE,
        -> SpanStyle(color = type)
        CodeTokenKind.COMMENT -> SpanStyle(color = comment, fontStyle = FontStyle.Italic)
    }
}

@Composable
internal fun rememberCodePalette(): CodePalette {
    val colors = AtlasTheme.colors
    return CodePalette(
        keyword = colors.primary,
        string = colors.tertiary,
        number = colors.error,
        type = colors.secondary,
        comment = colors.onSurfaceVariant,
    )
}
