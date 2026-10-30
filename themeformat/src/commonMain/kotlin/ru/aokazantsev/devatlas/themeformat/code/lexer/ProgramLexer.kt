package ru.aokazantsev.devatlas.themeformat.code.lexer

internal class ProgramLexer(private val rules: ProgramRules) : CodeLexer {
    override fun tokenize(state: LexerState) {
        ProgramScan(rules, state).run()
    }
}
