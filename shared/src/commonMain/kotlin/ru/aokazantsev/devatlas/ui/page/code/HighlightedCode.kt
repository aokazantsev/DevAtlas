package ru.aokazantsev.devatlas.ui.page.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import ru.aokazantsev.devatlas.themeformat.code.CodeHighlighter
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage

internal fun highlightedCode(code: String, language: CodeLanguage?, palette: CodePalette): AnnotatedString =
    buildAnnotatedString {
        append(code)
        if (language == null) return@buildAnnotatedString
        CodeHighlighter.highlight(code, language).forEach { span ->
            addStyle(palette.styleOf(span.kind), span.start, span.end)
        }
    }
