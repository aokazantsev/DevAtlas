package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class MarkdownLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        var fence: String? = null
        while (!state.atEnd) {
            val lineEnd = state.lineEnd()
            val contentStart = state.skipSpaces()
            val marker = FENCES.firstOrNull { contentStart - state.pos <= MAX_INDENT && state.startsWith(it, contentStart) }
            when {
                fence != null -> {
                    val closes = marker == fence
                    state.emit(contentStart, lineEnd, if (closes) CodeTokenKind.META else CodeTokenKind.STRING)
                    if (closes) fence = null
                }
                marker != null -> {
                    state.emit(contentStart, lineEnd, CodeTokenKind.META)
                    fence = marker
                }
                else -> {
                    state.pos = contentStart
                    line(state, lineEnd)
                }
            }
            state.pos = minOf(lineEnd + 1, state.end)
        }
    }

    private fun line(state: LexerState, lineEnd: Int) {
        val content = state.text.substring(state.pos, lineEnd)
        when {
            HEADING.matches(content) -> state.emitTo(lineEnd, CodeTokenKind.HEADING)
            content.startsWith(">") -> state.emitTo(lineEnd, CodeTokenKind.COMMENT)
            isRule(content) -> state.emitTo(lineEnd, CodeTokenKind.META)
            else -> {
                LIST_MARKER.find(content)?.let { state.emitTo(state.pos + it.value.trimEnd().length, CodeTokenKind.KEYWORD) }
                inline(state, lineEnd)
            }
        }
    }

    private fun isRule(content: String): Boolean {
        val marker = content.firstOrNull()?.takeIf { it in RULE_MARKERS } ?: return false
        return content.all { it == marker || it.isWhitespace() } && content.count { it == marker } >= MIN_RULE_MARKERS
    }

    private fun inline(state: LexerState, lineEnd: Int) {
        while (state.pos < lineEnd) {
            when (state.char()) {
                '`' -> delimited(state, '`', lineEnd, CodeTokenKind.STRING)
                '<' -> if (state.startsWith("<http")) delimited(state, '>', lineEnd, CodeTokenKind.STRING) else state.pos++
                ']' -> if (state.char(1) == '(') {
                    state.pos++
                    delimited(state, ')', lineEnd, CodeTokenKind.STRING)
                } else {
                    state.pos++
                }
                else -> state.pos++
            }
        }
    }

    private fun delimited(state: LexerState, close: Char, lineEnd: Int, kind: CodeTokenKind) {
        val found = (state.pos + 1 until lineEnd).firstOrNull { state.text[it] == close }
        if (found == null) state.pos++ else state.emitTo(found + 1, kind)
    }

    private companion object {
        const val MAX_INDENT = 3
        const val MIN_RULE_MARKERS = 3
        const val RULE_MARKERS = "-*_"
        val FENCES = listOf("```", "~~~")
        val HEADING = Regex("#{1,6}(\\s.*)?")
        val LIST_MARKER = Regex("^([-*+]|\\d{1,9}[.)])\\s")
    }
}
