package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class DiffLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        while (!state.atEnd) {
            val lineEnd = state.lineEnd()
            kind(state)?.let { state.emit(state.pos, lineEnd, it) }
            state.pos = minOf(lineEnd + 1, state.end)
        }
    }

    private fun kind(state: LexerState): CodeTokenKind? = when {
        HEADERS.any { state.startsWith(it) } -> CodeTokenKind.META
        state.startsWith("@@") -> CodeTokenKind.KEYWORD
        state.startsWith("+") -> CodeTokenKind.INSERTED
        state.startsWith("-") -> CodeTokenKind.DELETED
        state.startsWith("\\") -> CodeTokenKind.COMMENT
        else -> null
    }

    private companion object {
        val HEADERS = listOf(
            "+++ ", "--- ", "diff ", "index ", "new file", "deleted file", "similarity ", "rename ", "old mode", "new mode",
        )
    }
}
