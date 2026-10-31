package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class CssLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        CssScan(state).run()
    }

    private class CssScan(private val s: LexerState) {
        private val blocks = mutableListOf<Boolean>()
        private var preludeStart = s.pos
        private var expectProperty = false

        private val inDeclarations: Boolean
            get() = blocks.lastOrNull() == true

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
            val leadsPrelude = s.skipWhitespace(preludeStart) == s.pos
            s.emitTo(minOf(index + 2, s.end), CodeTokenKind.COMMENT)
            if (leadsPrelude) preludeStart = s.pos
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
                '{' -> blocks += !opensRuleGroup()
                '}' -> if (blocks.isNotEmpty()) blocks.removeAt(blocks.lastIndex)
                ';' -> Unit
                else -> return false
            }
            expectProperty = inDeclarations
            s.pos++
            preludeStart = s.pos
            return true
        }

        private fun opensRuleGroup(): Boolean {
            val first = s.skipWhitespace(preludeStart)
            if (s.charAt(first) != '@') return false
            val name = s.text.substring(first + 1, s.identifierEnd(first + 1, NAME_EXTRA)).lowercase()
            return name in RULE_GROUPS || name.endsWith("keyframes")
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
            val inValue = inDeclarations && !expectProperty
            s.emitTo(s.identifierEnd(s.pos + 1, NAME_EXTRA), if (inValue) CodeTokenKind.NUMBER else CodeTokenKind.TYPE)
            return true
        }

        private fun number(): Boolean {
            val char = s.char() ?: return false
            val signed = char == '-' && !LexerState.isIdentifierPart(s.previous(), NAME_EXTRA) && startsNumber(s.pos + 1)
            val fraction = char == '.' && s.char(1)?.isDigit() == true
            if (!char.isDigit() && !fraction && !signed) return false
            if (!inDeclarations || expectProperty) return false
            var end = s.numberEnd(if (signed) s.pos + 1 else s.pos)
            if (s.charAt(end) == '%') end++
            s.emitTo(end, CodeTokenKind.NUMBER)
            return true
        }

        private fun startsNumber(index: Int): Boolean =
            s.charAt(index)?.isDigit() == true || s.charAt(index) == '.' && s.charAt(index + 1)?.isDigit() == true

        private fun identifier(): Boolean {
            val selector = s.char() == '.' || s.char() == ':'
            val nameStart = if (selector) s.pos + 1 else s.pos
            val first = s.charAt(nameStart)
            if (!LexerState.isIdentifierStart(first, NAME_EXTRA)) return false
            val end = s.identifierEnd(nameStart, NAME_EXTRA)
            val kind = when {
                !selector && isPropertyName(end) -> CodeTokenKind.PROPERTY
                s.startsWith("--") -> CodeTokenKind.VARIABLE
                inDeclarations && !expectProperty -> null
                selector -> CodeTokenKind.TYPE
                else -> CodeTokenKind.TAG
            }
            if (kind == CodeTokenKind.PROPERTY) expectProperty = false
            if (kind == null) s.pos = end else s.emitTo(end, kind)
            return true
        }

        private fun isPropertyName(end: Int): Boolean =
            inDeclarations && expectProperty && s.charAt(s.skipSpaces(end)) == ':'
    }

    private companion object {
        const val NAME_EXTRA = "-"
        const val IMPORTANT_LENGTH = 10
        val RULE_GROUPS = words("media supports layer container document scope starting-style")
    }
}
