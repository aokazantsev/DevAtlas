package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class CssLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        CssScan(state).run()
    }

    private class CssScan(private val s: LexerState) {
        private var depth = 0
        private var expectProperty = false

        private val scanners: List<() -> Boolean> = listOf(
            ::comment,
            ::string,
            ::punctuation,
            ::atRule,
            ::important,
            ::hash,
            ::number,
            ::identifier,
        )

        fun run() {
            while (!s.atEnd) {
                if (scanners.none { it() }) s.pos++
            }
        }

        private fun comment(): Boolean {
            if (!s.startsWith("/*")) return false
            var index = s.pos + 2
            while (index < s.end && !s.startsWith("*/", index)) index++
            s.emitTo(minOf(index + 2, s.end), CodeTokenKind.COMMENT)
            return true
        }

        private fun string(): Boolean {
            val quote = s.char()
            if (quote != '"' && quote != '\'') return false
            var index = s.pos + 1
            while (index < s.end && s.text[index] != quote && s.text[index] != '\n') {
                index += if (s.text[index] == '\\') 2 else 1
            }
            s.emitTo(minOf(index + 1, s.end), CodeTokenKind.STRING)
            return true
        }

        private fun punctuation(): Boolean {
            when (s.char()) {
                '{' -> {
                    depth++
                    expectProperty = true
                }
                '}' -> {
                    depth = maxOf(0, depth - 1)
                    expectProperty = depth > 0
                }
                ';' -> expectProperty = depth > 0
                else -> return false
            }
            s.pos++
            return true
        }

        private fun atRule(): Boolean {
            if (s.char() != '@' || !LexerState.isIdentifierStart(s.char(1))) return false
            s.emitTo(s.identifierEnd(s.pos + 1, NAME_EXTRA), CodeTokenKind.KEYWORD)
            return true
        }

        private fun important(): Boolean {
            if (!s.startsWithIgnoreCase("!important")) return false
            s.emitTo(s.pos + IMPORTANT_LENGTH, CodeTokenKind.KEYWORD)
            return true
        }

        private fun hash(): Boolean {
            if (s.char() != '#' || !LexerState.isIdentifierPart(s.char(1), NAME_EXTRA)) return false
            val inValue = depth > 0 && !expectProperty
            s.emitTo(s.identifierEnd(s.pos + 1, NAME_EXTRA), if (inValue) CodeTokenKind.NUMBER else CodeTokenKind.TYPE)
            return true
        }

        private fun number(): Boolean {
            val char = s.char() ?: return false
            val fraction = char == '.' && s.char(1)?.isDigit() == true
            if (!char.isDigit() && !fraction) return false
            if (depth == 0 || expectProperty) return false
            var end = s.numberEnd(s.pos)
            if (s.charAt(end) == '%') end++
            s.emitTo(end, CodeTokenKind.NUMBER)
            return true
        }

        private fun identifier(): Boolean {
            val selector = s.char() == '.' || s.char() == ':'
            val nameStart = if (selector) s.pos + 1 else s.pos
            val first = s.charAt(nameStart)
            if (!LexerState.isIdentifierStart(first, NAME_EXTRA)) return false
            val end = s.identifierEnd(nameStart, NAME_EXTRA)
            val kind = when {
                !selector && isPropertyName(end) -> CodeTokenKind.PROPERTY
                s.startsWith("--") -> CodeTokenKind.VARIABLE
                depth > 0 && !expectProperty -> null
                selector -> CodeTokenKind.TYPE
                else -> CodeTokenKind.TAG
            }
            if (kind == CodeTokenKind.PROPERTY) expectProperty = false
            if (kind == null) s.pos = end else s.emitTo(end, kind)
            return true
        }

        private fun isPropertyName(end: Int): Boolean =
            depth > 0 && expectProperty && s.charAt(s.skipSpaces(end)) == ':'
    }

    private companion object {
        const val NAME_EXTRA = "-"
        const val IMPORTANT_LENGTH = 10
    }
}
