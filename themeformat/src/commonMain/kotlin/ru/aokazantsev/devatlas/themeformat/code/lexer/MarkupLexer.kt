package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind

internal class MarkupLexer(private val embedded: Map<String, CodeLexer> = emptyMap()) : CodeLexer {

    override fun tokenize(state: LexerState) {
        while (!state.atEnd) {
            when {
                state.startsWith("<!--") -> delimited(state, "-->", CodeTokenKind.COMMENT)
                state.startsWith("<![CDATA[") -> delimited(state, "]]>", CodeTokenKind.STRING)
                state.startsWith("<!") || state.startsWith("<?") -> delimited(state, ">", CodeTokenKind.META)
                state.char() == '<' && isTagStart(state) -> tag(state)
                state.char() == '&' -> entity(state)
                else -> state.pos++
            }
        }
    }

    private fun isTagStart(state: LexerState): Boolean {
        val next = if (state.char(1) == '/') state.char(2) else state.char(1)
        return next?.isLetter() == true
    }

    private fun delimited(state: LexerState, close: String, kind: CodeTokenKind) {
        val found = state.text.indexOf(close, state.pos + 2)
        val end = if (found == -1 || found + close.length > state.end) state.end else found + close.length
        state.emitTo(end, kind)
    }

    private fun entity(state: LexerState) {
        var index = state.pos + 1
        while (state.charAt(index)?.let { it.isLetterOrDigit() || it == '#' } == true) index++
        if (state.charAt(index) == ';' && index > state.pos + 1) state.emitTo(index + 1, CodeTokenKind.META) else state.pos++
    }

    private fun tag(state: LexerState) {
        val closing = state.char(1) == '/'
        val nameStart = state.pos + if (closing) 2 else 1
        val nameEnd = state.identifierEnd(nameStart, NAME_EXTRA)
        state.emitTo(nameEnd, CodeTokenKind.TAG)
        while (insideTag(state)) {
            when {
                state.char() == '"' || state.char() == '\'' -> attributeValue(state)
                LexerState.isIdentifierStart(state.char(), NAME_EXTRA) ->
                    state.emitTo(state.identifierEnd(state.pos, NAME_EXTRA), CodeTokenKind.ATTRIBUTE)
                else -> state.pos++
            }
        }
        val selfClosing = state.startsWith("/>")
        when {
            selfClosing -> state.emitTo(state.pos + 2, CodeTokenKind.TAG)
            state.char() == '>' -> state.emitTo(state.pos + 1, CodeTokenKind.TAG)
        }
        val name = state.text.substring(nameStart, nameEnd).lowercase()
        if (!closing && !selfClosing) embedded[name]?.let { embed(state, name, it) }
    }

    private fun insideTag(state: LexerState): Boolean {
        val char = state.char() ?: return false
        return char != '>' && char != '<' && !state.startsWith("/>")
    }

    private fun attributeValue(state: LexerState) {
        val quote = state.char() ?: return
        val found = state.text.indexOf(quote, state.pos + 1)
        val end = if (found == -1 || found >= state.end) state.end else found + 1
        state.emitTo(end, CodeTokenKind.STRING)
    }

    private fun embed(state: LexerState, name: String, lexer: CodeLexer) {
        val found = state.text.indexOf("</$name", state.pos, ignoreCase = true)
        val end = if (found == -1 || found > state.end) state.end else found
        lexer.tokenize(state.embedded(state.pos, end))
        state.pos = end
    }

    private companion object {
        const val NAME_EXTRA = ":-."
    }
}
