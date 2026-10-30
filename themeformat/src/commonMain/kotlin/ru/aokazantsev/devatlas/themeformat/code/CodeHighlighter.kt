package ru.aokazantsev.devatlas.themeformat.code

import ru.aokazantsev.devatlas.themeformat.code.lexer.CodeLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.CssLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.DiffLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.JsonLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.JvmFamilyRules
import ru.aokazantsev.devatlas.themeformat.code.lexer.LexerState
import ru.aokazantsev.devatlas.themeformat.code.lexer.MarkdownLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.MarkupLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.NativeFamilyRules
import ru.aokazantsev.devatlas.themeformat.code.lexer.ProgramLexer
import ru.aokazantsev.devatlas.themeformat.code.lexer.ScriptFamilyRules
import ru.aokazantsev.devatlas.themeformat.code.lexer.YamlLexer

object CodeHighlighter {

    const val MAX_CODE_LENGTH = 20_000

    private val css = CssLexer()
    private val javaScript = ProgramLexer(ScriptFamilyRules.javaScript)

    private val lexers: Map<CodeLanguage, CodeLexer> = mapOf(
        CodeLanguage.KOTLIN to ProgramLexer(JvmFamilyRules.kotlin),
        CodeLanguage.JAVA to ProgramLexer(JvmFamilyRules.java),
        CodeLanguage.GROOVY to ProgramLexer(JvmFamilyRules.groovy),
        CodeLanguage.DART to ProgramLexer(JvmFamilyRules.dart),
        CodeLanguage.SWIFT to ProgramLexer(NativeFamilyRules.swift),
        CodeLanguage.OBJECTIVE_C to ProgramLexer(NativeFamilyRules.objectiveC),
        CodeLanguage.C to ProgramLexer(NativeFamilyRules.c),
        CodeLanguage.CPP to ProgramLexer(NativeFamilyRules.cpp),
        CodeLanguage.CSHARP to ProgramLexer(NativeFamilyRules.csharp),
        CodeLanguage.GO to ProgramLexer(NativeFamilyRules.go),
        CodeLanguage.RUST to ProgramLexer(NativeFamilyRules.rust),
        CodeLanguage.JAVASCRIPT to javaScript,
        CodeLanguage.TYPESCRIPT to ProgramLexer(ScriptFamilyRules.typeScript),
        CodeLanguage.PYTHON to ProgramLexer(ScriptFamilyRules.python),
        CodeLanguage.RUBY to ProgramLexer(ScriptFamilyRules.ruby),
        CodeLanguage.PHP to ProgramLexer(ScriptFamilyRules.php),
        CodeLanguage.SHELL to ProgramLexer(ScriptFamilyRules.shell),
        CodeLanguage.SQL to ProgramLexer(ScriptFamilyRules.sql),
        CodeLanguage.JSON to JsonLexer(),
        CodeLanguage.YAML to YamlLexer(),
        CodeLanguage.XML to MarkupLexer(),
        CodeLanguage.HTML to MarkupLexer(mapOf("script" to javaScript, "style" to css)),
        CodeLanguage.CSS to css,
        CodeLanguage.MARKDOWN to MarkdownLexer(),
        CodeLanguage.DIFF to DiffLexer(),
    )

    fun highlight(code: String, language: CodeLanguage): List<CodeSpan> {
        if (code.length > MAX_CODE_LENGTH) return emptyList()
        val lexer = lexers[language] ?: return emptyList()
        val spans = mutableListOf<CodeSpan>()
        lexer.tokenize(LexerState(code, 0, code.length, spans))
        return spans
    }
}
