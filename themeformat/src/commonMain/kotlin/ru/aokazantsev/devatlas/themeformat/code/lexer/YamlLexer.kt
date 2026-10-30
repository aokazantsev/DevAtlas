package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class YamlLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        var blockIndent: Int? = null
        while (!state.atEnd) {
            val lineEnd = state.lineEnd()
            val contentStart = state.skipSpaces()
            val indent = contentStart - state.pos
            val blank = contentStart >= lineEnd
            val inBlock = blockIndent != null && (blank || indent > blockIndent)
            if (inBlock) {
                state.emit(contentStart, lineEnd, CodeTokenKind.STRING)
            } else {
                state.pos = contentStart
                blockIndent = line(state, lineEnd, indent)
            }
            state.pos = minOf(lineEnd + 1, state.end)
        }
    }

    private fun line(state: LexerState, lineEnd: Int, indent: Int): Int? {
        val contentStart = state.pos
        if (state.char() == '#') {
            state.emitTo(lineEnd, CodeTokenKind.COMMENT)
            return null
        }
        if (indent == 0 && (state.startsWith("---") || state.startsWith("..."))) {
            state.emitTo(state.pos + DOCUMENT_MARKER, CodeTokenKind.META)
        }
        while (state.char() == '-' && (state.char(1) == ' ' || state.pos + 1 == lineEnd)) {
            state.emitTo(state.pos + 1, CodeTokenKind.KEYWORD)
            state.pos = state.skipSpaces()
        }
        var blockIndent = indent
        keyEnd(state, lineEnd)?.let { end ->
            blockIndent = indent + state.pos - contentStart
            state.emitTo(end, CodeTokenKind.PROPERTY)
            state.pos = state.skipSpaces(end + 1)
        }
        return if (value(state, lineEnd)) blockIndent else null
    }

    private fun keyEnd(state: LexerState, lineEnd: Int): Int? {
        val first = state.char() ?: return null
        return when {
            first == '"' || first == '\'' -> quotedKeyEnd(state, first, lineEnd)
            first in NOT_KEY_START -> null
            else -> plainKeyEnd(state, lineEnd)
        }
    }

    private fun quotedKeyEnd(state: LexerState, quote: Char, lineEnd: Int): Int? {
        val closing = state.text.indexOf(quote, state.pos + 1)
        if (closing == -1 || closing >= lineEnd) return null
        val colon = state.skipSpaces(closing + 1)
        return colon.takeIf { state.charAt(it) == ':' && isKeyColon(state, it, lineEnd) }
    }

    private fun plainKeyEnd(state: LexerState, lineEnd: Int): Int? {
        var index = state.pos
        while (index < lineEnd) {
            val char = state.text[index]
            if (char == '#' && state.text[index - 1].isWhitespace()) return null
            if (char == ':' && isKeyColon(state, index, lineEnd)) return index
            index++
        }
        return null
    }

    private fun isKeyColon(state: LexerState, index: Int, lineEnd: Int): Boolean =
        index + 1 >= lineEnd || state.text[index + 1].isWhitespace()

    private fun value(state: LexerState, lineEnd: Int): Boolean {
        if (state.pos >= lineEnd) return false
        val first = state.char()
        if (first == '|' || first == '>') {
            state.emitTo(tokenEnd(state, lineEnd), CodeTokenKind.KEYWORD)
            comment(state, lineEnd)
            return true
        }
        state.pos = state.skipSpaces()
        while (state.pos < lineEnd && !comment(state, lineEnd)) {
            valueToken(state, lineEnd)
            state.pos = state.skipSpaces()
        }
        return false
    }

    private fun comment(state: LexerState, lineEnd: Int): Boolean {
        val start = state.skipSpaces()
        if (state.charAt(start) != '#') return false
        state.emit(start, lineEnd, CodeTokenKind.COMMENT)
        state.pos = lineEnd
        return true
    }

    private fun valueToken(state: LexerState, lineEnd: Int) {
        val char = state.char() ?: return
        when {
            char == '"' || char == '\'' -> {
                val closing = state.text.indexOf(char, state.pos + 1)
                state.emitTo(if (closing == -1 || closing >= lineEnd) lineEnd else closing + 1, CodeTokenKind.STRING)
            }
            char == '&' || char == '*' -> state.emitTo(tokenEnd(state, lineEnd), CodeTokenKind.VARIABLE)
            char == '!' -> state.emitTo(tokenEnd(state, lineEnd), CodeTokenKind.META)
            char in FLOW_PUNCTUATION -> state.pos++
            else -> scalar(state, lineEnd)
        }
    }

    private fun scalar(state: LexerState, lineEnd: Int) {
        val end = tokenEnd(state, lineEnd)
        val word = state.text.substring(state.pos, end)
        if (word.lowercase() in LITERALS || NUMBER.matches(word)) state.emitTo(end, CodeTokenKind.NUMBER) else state.pos = end
    }

    private fun tokenEnd(state: LexerState, lineEnd: Int): Int {
        var index = state.pos + 1
        while (index < lineEnd && !state.text[index].isWhitespace() && state.text[index] !in FLOW_PUNCTUATION) index++
        return index
    }

    private companion object {
        const val DOCUMENT_MARKER = 3
        const val NOT_KEY_START = "[{|>&*!%@`#"
        const val FLOW_PUNCTUATION = ",[]{}"
        val LITERALS = setOf("true", "false", "null", "yes", "no", "on", "off", "~")
        val NUMBER = Regex(
            "[-+]?(\\d[\\d_]*(\\.\\d*)?([eE][-+]?\\d+)?|\\.\\d+|0x[0-9a-fA-F_]+|0o[0-7_]+|\\.inf|\\.nan)",
            RegexOption.IGNORE_CASE,
        )
    }
}
