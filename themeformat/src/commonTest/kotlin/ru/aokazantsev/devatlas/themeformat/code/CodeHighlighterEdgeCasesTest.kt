package ru.aokazantsev.devatlas.themeformat.code

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.COMMENT
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.DELETED
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.INSERTED
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.KEYWORD
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.META
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.NUMBER
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.PROPERTY
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.STRING
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.TAG
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.TYPE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeHighlighterEdgeCasesTest {

    private fun tokens(code: String, language: CodeLanguage): List<Pair<String, CodeTokenKind>> =
        CodeHighlighter.highlight(code, language).map { code.substring(it.start, it.end).trimEnd('\n') to it.kind }

    private fun assertHas(code: String, language: CodeLanguage, vararg expected: Pair<String, CodeTokenKind>) {
        val actual = tokens(code, language)
        expected.forEach { assertTrue(it in actual, "$language: нет $it в $actual") }
    }

    private fun assertHasNot(code: String, language: CodeLanguage, vararg unexpected: Pair<String, CodeTokenKind>) {
        val actual = tokens(code, language)
        unexpected.forEach { assertFalse(it in actual, "$language: лишнее $it в $actual") }
    }

    @Test
    fun cppDigitSeparatorStaysInsideNumber() {
        val code = "int n = 1'000'000;\nunsigned m = 0xFF'FF;\nchar c = 'a';"
        assertHas(code, CodeLanguage.CPP, "1'000'000" to NUMBER, "0xFF'FF" to NUMBER, "'a'" to STRING)
        assertHas("int n = 1'000;\nint k = 2;", CodeLanguage.C, "1'000" to NUMBER, "2" to NUMBER)
    }

    @Test
    fun markdownLongFenceKeepsInnerFenceAsContent() {
        val code = "````markdown\n```kotlin\nval a = 1\n```\n````\n# after"
        assertHas(
            code,
            CodeLanguage.MARKDOWN,
            "````markdown" to META,
            "```kotlin" to STRING,
            "val a = 1" to STRING,
            "```" to STRING,
            "````" to META,
            "# after" to CodeTokenKind.HEADING,
        )
    }

    @Test
    fun markdownFenceWithInfoStringClosesOnlyOnBareFence() {
        val code = "```kotlin\nval a = 1\n``` not a close\n```\n- item"
        assertHas(
            code,
            CodeLanguage.MARKDOWN,
            "```kotlin" to META,
            "val a = 1" to STRING,
            "``` not a close" to STRING,
            "```" to META,
            "-" to KEYWORD,
        )
    }

    @Test
    fun markdownTildeFenceAndInlineTripleBackticks() {
        assertHas("~~~~\n~~~\ntext\n~~~~\nplain", CodeLanguage.MARKDOWN, "~~~" to STRING, "text" to STRING, "~~~~" to META)
        assertHas("use ```code``` inline", CodeLanguage.MARKDOWN, "```code```" to STRING)
        assertHasNot("use ```code``` inline", CodeLanguage.MARKDOWN, "use ```code``` inline" to META)
    }

    @Test
    fun cssSignedNumbersAreWholeTokens() {
        val code = "a { margin: 0 -8px; top: -.5em; width: calc(100% - 4px); }"
        assertHas(code, CodeLanguage.CSS, "-8px" to NUMBER, "-.5em" to NUMBER, "100%" to NUMBER, "4px" to NUMBER)
        assertHasNot(code, CodeLanguage.CSS, "8px" to NUMBER)
    }

    @Test
    fun cssSelectorInsideMediaIsNotProperty() {
        val code = "@media (min-width: 600px) {\n  a:hover { color: red; }\n  .card { margin: -4px; }\n}\nb { top: 0; }"
        assertHas(
            code,
            CodeLanguage.CSS,
            "@media" to KEYWORD,
            "a" to TAG,
            ":hover" to TYPE,
            "color" to PROPERTY,
            ".card" to TYPE,
            "margin" to PROPERTY,
            "-4px" to NUMBER,
            "b" to TAG,
            "top" to PROPERTY,
        )
        assertHasNot(code, CodeLanguage.CSS, "a" to PROPERTY, "hover" to PROPERTY)
    }

    @Test
    fun cssKeyframesAndFontFace() {
        val code = "@keyframes spin { from { opacity: 0 } to { opacity: 1 } }\n@font-face { font-family: X; }"
        assertHas(code, CodeLanguage.CSS, "from" to TAG, "to" to TAG, "opacity" to PROPERTY, "font-family" to PROPERTY)
    }

    @Test
    fun phpAttributesAreNotComments() {
        val code = "<?php\n#[Route('/x')]\nfunction f() {} # note\n#[\n    Attr,\n]\nclass A {}"
        assertHas(code, CodeLanguage.PHP, "#[Route('/x')]" to META, "# note" to COMMENT, "class" to KEYWORD)
        assertHasNot(code, CodeLanguage.PHP, "#[Route('/x')]" to COMMENT)
    }

    @Test
    fun diffDashLinesInsideHunkAreContent() {
        val code = "diff --git a/a b/a\n--- a/a\n+++ b/a\n@@ -1,3 +1,3 @@\n keep\n--- removed\n+++ added\n tail"
        assertHas(
            code,
            CodeLanguage.DIFF,
            "diff --git a/a b/a" to META,
            "--- a/a" to META,
            "+++ b/a" to META,
            "@@ -1,3 +1,3 @@" to KEYWORD,
            "--- removed" to DELETED,
            "+++ added" to INSERTED,
        )
    }

    @Test
    fun diffHeadersResumeAfterHunkEnds() {
        val code = "--- a/a\n+++ b/a\n@@ -1 +1 @@\n-x\n+y\n--- a/b\n+++ b/b\n@@ -1,2 +1,2 @@\n-p\n-- q\n+r\n++ s"
        assertHas(
            code,
            CodeLanguage.DIFF,
            "--- a/a" to META,
            "-x" to DELETED,
            "+y" to INSERTED,
            "--- a/b" to META,
            "+++ b/b" to META,
            "-- q" to DELETED,
            "++ s" to INSERTED,
        )
    }

    @Test
    fun pythonStringPrefixes() {
        val code = "a = r\"\\d+\\\"x\"\nb = rb'\\x00'\nc = f\"v={n}\"\nd = Rb\"\"\"raw\n\"\"\"\ne = 1"
        assertHas(
            code,
            CodeLanguage.PYTHON,
            "r\"\\d+\\\"x\"" to STRING,
            "rb'\\x00'" to STRING,
            "f\"v={n}\"" to STRING,
            "Rb\"\"\"raw\n\"\"\"" to STRING,
            "1" to NUMBER,
        )
    }

    @Test
    fun dartRawStringsIgnoreEscapesAndTemplates() {
        val code = "var a = r'C:\\dir\\';\nvar b = r'\$x';\nvar c = 'ok \$x';"
        val actual = tokens(code, CodeLanguage.DART)
        assertEquals(3, actual.count { it == "var" to KEYWORD }, "$actual")
        assertTrue("r'C:\\dir\\'" to STRING in actual, "$actual")
        assertTrue("r'\$x'" to STRING in actual, "$actual")
        assertTrue("\$x" to CodeTokenKind.VARIABLE in actual, "$actual")
        assertEquals(1, actual.count { it.second == CodeTokenKind.VARIABLE }, "$actual")
    }

    @Test
    fun rubyDefinedPredicateIsOneKeyword() {
        assertHas("puts 1 if defined?(x)\nnil?\nfoo!", CodeLanguage.RUBY, "defined?" to KEYWORD)
        assertHasNot("defined?(x)", CodeLanguage.RUBY, "defined" to KEYWORD)
    }

    @Test
    fun rubyPercentLiterals() {
        val code = "a = %r{a/b}i\nb = %w[x y]\nc = %q(a (nested) b)\nd = %Q<x>\ne = %(plain)\nf = a % b\ng = 1"
        assertHas(
            code,
            CodeLanguage.RUBY,
            "%r{a/b}i" to STRING,
            "%w[x y]" to STRING,
            "%q(a (nested) b)" to STRING,
            "%Q<x>" to STRING,
            "%(plain)" to STRING,
            "1" to NUMBER,
        )
        assertEquals(5, tokens(code, CodeLanguage.RUBY).count { it.second == STRING })
    }

    @Test
    fun twoHeredocsOnOneLineAreBothStrings() {
        val shell = "cat <<A <<B\nfirst\nA\nsecond\nB\nexit 1"
        assertHas(shell, CodeLanguage.SHELL, "first" to STRING, "second" to STRING, "<<A" to META, "<<B" to META, "exit" to KEYWORD)
        val ruby = "puts(<<~A, <<~B)\n  one\nA\n  two\nB\nputs 1"
        assertHas(ruby, CodeLanguage.RUBY, "  one" to STRING, "  two" to STRING, "<<~A" to META, "<<~B" to META, "1" to NUMBER)
    }

    @Test
    fun singleHeredocStillWorks() {
        assertHas("cat <<EOF\nbody\nEOF\nexit 0", CodeLanguage.SHELL, "body" to STRING, "exit" to KEYWORD)
    }
}
