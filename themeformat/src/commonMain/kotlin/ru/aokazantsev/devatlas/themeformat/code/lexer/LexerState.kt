package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeSpan
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class LexerState(
    val text: String,
    val start: Int,
    val end: Int,
    private val out: MutableList<CodeSpan>,
) {
    var pos: Int = start

    val atEnd: Boolean
        get() = pos >= end

    fun char(offset: Int = 0): Char? = charAt(pos + offset)

    fun charAt(index: Int): Char? = if (index in start until end) text[index] else null

    fun previous(): Char? = if (pos > start) text[pos - 1] else null

    fun startsWith(prefix: String, at: Int = pos): Boolean =
        at + prefix.length <= end && text.startsWith(prefix, at)

    fun startsWithIgnoreCase(prefix: String, at: Int = pos): Boolean =
        at + prefix.length <= end && text.startsWith(prefix, at, ignoreCase = true)

    fun emit(from: Int, to: Int, kind: CodeTokenKind) {
        if (to > from) out += CodeSpan(from, to, kind)
    }

    fun emitTo(to: Int, kind: CodeTokenKind) {
        emit(pos, to, kind)
        pos = to
    }

    fun lineEnd(from: Int = pos): Int {
        var index = from
        while (index < end && text[index] != '\n') index++
        return index
    }

    fun isLineStart(at: Int = pos): Boolean {
        var index = at - 1
        while (index >= start && text[index] != '\n') {
            if (!text[index].isWhitespace()) return false
            index--
        }
        return true
    }

    fun embedded(from: Int, to: Int): LexerState = LexerState(text, from, to, out)

    companion object {
        fun isIdentifierStart(char: Char?, extra: String = ""): Boolean =
            char != null && (char.isLetter() || char == '_' || char in extra)

        fun isIdentifierPart(char: Char?, extra: String = ""): Boolean =
            isIdentifierStart(char, extra) || char?.isDigit() == true
    }
}
