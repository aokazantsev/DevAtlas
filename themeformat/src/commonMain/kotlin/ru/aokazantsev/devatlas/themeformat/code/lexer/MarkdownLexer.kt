package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class MarkdownLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        var fence: Fence? = null
        while (!state.atEnd) {
            val lineEnd = state.lineEnd()
            val contentStart = state.skipSpaces()
            val candidate = if (contentStart - state.pos <= MAX_INDENT) fenceAt(state, contentStart, lineEnd) else null
            when {
                fence != null -> {
                    val closes = candidate != null && candidate.char == fence.char && candidate.length >= fence.length &&
                        candidate.info.isEmpty()
                    state.emit(contentStart, lineEnd, if (closes) CodeTokenKind.META else CodeTokenKind.STRING)
                    if (closes) fence = null
                }
                candidate != null -> {
                    state.emit(contentStart, lineEnd, CodeTokenKind.META)
                    fence = candidate
                }
                else -> {
                    state.pos = contentStart
                    line(state, lineEnd)
                }
            }
            state.pos = minOf(lineEnd + 1, state.end)
        }
    }

    private fun fenceAt(state: LexerState, from: Int, lineEnd: Int): Fence? {
        val char = state.charAt(from)?.takeIf { it in FENCE_CHARS } ?: return null
        var runEnd = from
        while (runEnd < lineEnd && state.text[runEnd] == char) runEnd++
        val length = runEnd - from
        val info = state.text.substring(runEnd, lineEnd).trim()
        if (length < MIN_FENCE || char == '`' && '`' in info) return null
        return Fence(char, length, info)
    }

    private class Fence(val char: Char, val length: Int, val info: String)

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
                '`' -> codeSpan(state, lineEnd)
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

    private fun codeSpan(state: LexerState, lineEnd: Int) {
        var runEnd = state.pos
        while (runEnd < lineEnd && state.text[runEnd] == '`') runEnd++
        val run = state.text.substring(state.pos, runEnd)
        var search = runEnd
        while (search < lineEnd) {
            val found = state.text.indexOf(run, search)
            if (found == -1 || found + run.length > lineEnd) break
            val after = found + run.length
            if (state.charAt(after) != '`' && state.text[found - 1] != '`') {
                state.emitTo(after, CodeTokenKind.STRING)
                return
            }
            search = after
        }
        state.pos = runEnd
    }

    private fun delimited(state: LexerState, close: Char, lineEnd: Int, kind: CodeTokenKind) {
        val found = (state.pos + 1 until lineEnd).firstOrNull { state.text[it] == close }
        if (found == null) state.pos++ else state.emitTo(found + 1, kind)
    }

    private companion object {
        const val MAX_INDENT = 3
        const val MIN_RULE_MARKERS = 3
        const val RULE_MARKERS = "-*_"
        const val MIN_FENCE = 3
        const val FENCE_CHARS = "`~"
        val HEADING = Regex("#{1,6}(\\s.*)?")
        val LIST_MARKER = Regex("^([-*+]|\\d{1,9}[.)])\\s")
    }
}
