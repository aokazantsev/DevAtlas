package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class DiffLexer : CodeLexer {

    override fun tokenize(state: LexerState) {
        val scan = DiffScan(state)
        while (!state.atEnd) {
            scan.line(state.lineEnd())
        }
    }

    private class DiffScan(private val s: LexerState) {
        private var oldLeft = 0
        private var newLeft = 0

        fun line(lineEnd: Int) {
            val inHunk = oldLeft > 0 || newLeft > 0
            when {
                s.startsWith("@@") -> hunkHeader(lineEnd)
                s.startsWith("diff ") -> {
                    oldLeft = 0
                    newLeft = 0
                    s.emit(s.pos, lineEnd, CodeTokenKind.META)
                }
                !inHunk && FILE_HEADERS.any { s.startsWith(it) } -> s.emit(s.pos, lineEnd, CodeTokenKind.META)
                HEADERS.any { s.startsWith(it) } -> s.emit(s.pos, lineEnd, CodeTokenKind.META)
                s.startsWith("+") -> {
                    newLeft = maxOf(0, newLeft - 1)
                    s.emit(s.pos, lineEnd, CodeTokenKind.INSERTED)
                }
                s.startsWith("-") -> {
                    oldLeft = maxOf(0, oldLeft - 1)
                    s.emit(s.pos, lineEnd, CodeTokenKind.DELETED)
                }
                s.startsWith("\\") -> s.emit(s.pos, lineEnd, CodeTokenKind.COMMENT)
                else -> {
                    oldLeft = maxOf(0, oldLeft - 1)
                    newLeft = maxOf(0, newLeft - 1)
                }
            }
            s.pos = minOf(lineEnd + 1, s.end)
        }

        private fun hunkHeader(lineEnd: Int) {
            val counts = HUNK_HEADER.find(s.text.substring(s.pos, lineEnd))
            oldLeft = count(counts, 1)
            newLeft = count(counts, 2)
            s.emit(s.pos, lineEnd, CodeTokenKind.KEYWORD)
        }

        private fun count(match: MatchResult?, group: Int): Int =
            match?.groupValues?.get(group)?.let { it.ifEmpty { "1" }.toInt() } ?: UNBOUNDED
    }

    private companion object {
        const val UNBOUNDED = Int.MAX_VALUE
        val HUNK_HEADER = Regex("^@@ -\\d+(?:,(\\d+))? \\+\\d+(?:,(\\d+))? @@")
        val FILE_HEADERS = listOf("+++ ", "--- ")
        val HEADERS = listOf(
            "index ", "new file", "deleted file", "similarity ", "rename ", "old mode", "new mode",
        )
    }
}
