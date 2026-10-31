package ru.aokazantsev.devatlas.ui.page.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import ru.aokazantsev.devatlas.themeformat.code.CodeHighlighter
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage
import ru.aokazantsev.devatlas.themeformat.code.CodeSpan

internal fun codeSpans(code: String, language: CodeLanguage?): List<CodeSpan> =
    language?.let { CodeHighlighter.highlight(code, it) }.orEmpty()

internal fun highlightedCode(code: String, spans: List<CodeSpan>, palette: CodePalette): AnnotatedString =
    buildAnnotatedString {
        append(code)
        spans.forEach { span -> addStyle(palette.styleOf(span.kind), span.start, span.end) }
    }
