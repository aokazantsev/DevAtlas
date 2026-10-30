package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class JsonLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        while (!state.atEnd) {
            val char = state.char() ?: return
            when {
                char == '"' -> string(state)
                char == '-' || char.isDigit() -> {
                    val digits = if (char == '-') state.pos + 1 else state.pos
                    state.emitTo(maxOf(state.numberEnd(digits), state.pos + 1), CodeTokenKind.NUMBER)
                }
                char.isLetter() -> word(state)
                else -> state.pos++
            }
        }
    }

    private fun string(state: LexerState) {
        var index = state.pos + 1
        while (index < state.end && state.text[index] != '"' && state.text[index] != '\n') {
            index += if (state.text[index] == '\\') 2 else 1
        }
        val end = minOf(index + 1, state.end)
        val key = state.charAt(skipWhitespace(state, end)) == ':'
        state.emitTo(end, if (key) CodeTokenKind.PROPERTY else CodeTokenKind.STRING)
    }

    private fun word(state: LexerState) {
        val end = state.identifierEnd(state.pos)
        val word = state.text.substring(state.pos, end)
        if (word in LITERALS) state.emitTo(end, CodeTokenKind.NUMBER) else state.pos = end
    }

    private fun skipWhitespace(state: LexerState, from: Int): Int {
        var index = from
        while (state.charAt(index)?.isWhitespace() == true) index++
        return index
    }

    private companion object {
        val LITERALS = setOf("true", "false", "null")
    }
}
