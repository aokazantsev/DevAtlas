package ru.aokazantsev.devatlas.themeformat.code

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.ATTRIBUTE
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.COMMENT
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.DELETED
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.HEADING
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.INSERTED
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.KEYWORD
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.META
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.NUMBER
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.PROPERTY
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.STRING
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.TAG
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.TYPE
import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind.VARIABLE
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeHighlighterTest {

    private fun tokens(code: String, language: CodeLanguage): List<Pair<String, CodeTokenKind>> =
        CodeHighlighter.highlight(code, language).map { code.substring(it.start, it.end) to it.kind }

    private fun assertTokens(code: String, language: CodeLanguage, vararg expected: Pair<String, CodeTokenKind>) {
        val actual = tokens(code, language)
        expected.forEach { assertTrue(it in actual, "$language: нет $it в $actual") }
    }

    private fun assertPlain(code: String, language: CodeLanguage, vararg words: String) {
        val actual = tokens(code, language).map { it.first }
        words.forEach { word -> assertTrue(actual.none { word in it }, "$language: «$word» подсвечено в $actual") }
    }

    @Test
    fun kotlinKeywordsStringsTemplatesAndAnnotations() {
        val code = "@Composable\nfun greet(name: String) = \"Hi, \$name \${name.length}\" // note\nval n = 1_000L"
        assertTokens(
            code,
            CodeLanguage.KOTLIN,
            "@Composable" to META,
            "fun" to KEYWORD,
            "String" to TYPE,
            "\"Hi, " to STRING,
            "\$name" to VARIABLE,
            "\${name.length}" to VARIABLE,
            "// note" to COMMENT,
            "1_000L" to NUMBER,
        )
        assertPlain(code, CodeLanguage.KOTLIN, "greet")
    }

    @Test
    fun kotlinSoftKeywordsUsedAsNamesStayPlain() {
        assertPlain("val v = it.value + map.get(key) + file.field", CodeLanguage.KOTLIN, "value", "get", "file", "field")
    }

    @Test
    fun kotlinRawStringAndNestedComment() {
        assertTokens(
            "/* a /* b */ c */ val s = \"\"\"x\\n\"\"\"",
            CodeLanguage.KOTLIN,
            "/* a /* b */ c */" to COMMENT,
            "\"\"\"x\\n\"\"\"" to STRING,
        )
    }

    @Test
    fun rangeIsNotFraction() {
        assertTokens("for (i in 1..10) {}", CodeLanguage.KOTLIN, "1" to NUMBER, "10" to NUMBER)
    }

    @Test
    fun escapedQuoteStaysInsideString() {
        assertTokens("val s = \"a\\\"b\"; val t = 2", CodeLanguage.JAVA, "\"a\\\"b\"" to STRING, "2" to NUMBER)
    }

    @Test
    fun unterminatedStringEndsAtLineEnd() {
        assertTokens("x = \"open\nint y = 3", CodeLanguage.JAVA, "\"open" to STRING, "int" to TYPE, "3" to NUMBER)
    }

    @Test
    fun javaScriptRegexTemplatesAndDivision() {
        val code = "const re = /a\\/b[/]/g; const half = total / 2 / 3; const s = `x \${a + 1}`"
        assertTokens(
            code,
            CodeLanguage.JAVASCRIPT,
            "const" to KEYWORD,
            "/a\\/b[/]/g" to STRING,
            "2" to NUMBER,
            "\${a + 1}" to VARIABLE,
        )
        assertPlain(code, CodeLanguage.JAVASCRIPT, "total")
    }

    @Test
    fun typeScriptTypes() {
        assertTokens(
            "interface User { name: string }",
            CodeLanguage.TYPESCRIPT,
            "interface" to KEYWORD,
            "User" to TYPE,
            "string" to TYPE,
        )
    }

    @Test
    fun pythonPrefixesDecoratorsAndDocstrings() {
        assertTokens(
            "@dataclass\ndef f(x):\n    \"\"\"Doc\"\"\"\n    return f\"{x}\" # c\nNone",
            CodeLanguage.PYTHON,
            "@dataclass" to META,
            "def" to KEYWORD,
            "\"\"\"Doc\"\"\"" to STRING,
            "f\"{x}\"" to STRING,
            "# c" to COMMENT,
            "None" to NUMBER,
        )
    }

    @Test
    fun rustLifetimesCharsRawStringsAndMacros() {
        assertTokens(
            "#[derive(Debug)]\nfn f<'a>(s: &'a str) { let c = '\\n'; let r = r#\"x\"y\"#; println!(\"{}\", c); }",
            CodeLanguage.RUST,
            "#[derive(Debug)]" to META,
            "'a" to TYPE,
            "'\\n'" to STRING,
            "r#\"x\"y\"#" to STRING,
            "println!" to META,
            "str" to TYPE,
        )
    }

    @Test
    fun cPreprocessorAndCppRawString() {
        assertTokens("#include <stdio.h>\nint main() { return 0; }", CodeLanguage.C, "#include <stdio.h>" to META, "int" to TYPE)
        assertTokens("auto s = R\"(a\"b)\";", CodeLanguage.CPP, "R\"(a\"b)\"" to STRING)
    }

    @Test
    fun objectiveCAtKeywordsAndStrings() {
        assertTokens(
            "@interface Foo : NSObject\n@property NSString *name = @\"x\";\n@end",
            CodeLanguage.OBJECTIVE_C,
            "@interface" to KEYWORD,
            "@\"x\"" to STRING,
            "NSObject" to TYPE,
        )
    }

    @Test
    fun swiftInterpolationAndCsharpVerbatim() {
        assertTokens("let s = \"a \\(b) c\"", CodeLanguage.SWIFT, "let" to KEYWORD, "\\(b)" to VARIABLE)
        assertTokens("var p = @\"C:\\dir\";", CodeLanguage.CSHARP, "@\"C:\\dir\"" to STRING)
    }

    @Test
    fun csharpInterpolatedAndRawStrings() {
        assertTokens(
            "var s = \$\"Hi {name}\"; var r = \"\"\"a \"b\" c\"\"\"; // c",
            CodeLanguage.CSHARP,
            "var" to KEYWORD,
            "\$\"Hi {name}\"" to STRING,
            "\"\"\"a \"b\" c\"\"\"" to STRING,
            "// c" to COMMENT,
        )
    }

    @Test
    fun goRawStringsAndBuiltins() {
        assertTokens("var s string = `a\nb`", CodeLanguage.GO, "string" to TYPE, "`a\nb`" to STRING)
    }

    @Test
    fun shellVariablesAndComments() {
        val code = "echo \"\$HOME\" \$1 # comment\nurl=a#b"
        assertTokens(code, CodeLanguage.SHELL, "\$HOME" to VARIABLE, "\$1" to VARIABLE, "# comment" to COMMENT)
        assertTrue(tokens(code, CodeLanguage.SHELL).none { it.first == "#b" })
    }

    @Test
    fun rubySymbolsAndInstanceVariables() {
        assertTokens("def a\n  @name = :sym # x\nend", CodeLanguage.RUBY, "def" to KEYWORD, "@name" to VARIABLE, ":sym" to NUMBER)
    }

    @Test
    fun rubyRegexLiteralsDoNotSwallowCode() {
        val code = "if line =~ /#{word}/i then count += 1 end\ns.gsub(/\"/, '')\nputs count / 2"
        assertTokens(code, CodeLanguage.RUBY, "/#{word}/i" to STRING, "/\"/" to STRING, "''" to STRING, "puts" to KEYWORD, "2" to NUMBER)
        assertTrue(tokens(code, CodeLanguage.RUBY).none { it.second == CodeTokenKind.COMMENT })
        val cases = "case line\nwhen /#{prefix}\\d+/ then puts \"x\"\nwhen /don't/ then y\nend"
        assertTokens(cases, CodeLanguage.RUBY, "then" to KEYWORD, "/don't/" to STRING, "end" to KEYWORD)
        assertTrue(tokens(cases, CodeLanguage.RUBY).none { it.second == CodeTokenKind.COMMENT })
    }

    @Test
    fun phpMarkersAndVariables() {
        assertTokens("<?php echo \$name; ?>", CodeLanguage.PHP, "<?php" to META, "echo" to KEYWORD, "\$name" to VARIABLE)
    }

    @Test
    fun phpTemplateHtmlBetweenTagsIsMarkup() {
        assertTokens(
            "<?php if (\$a): ?>\n<p>it's you</p>\n<?php echo \$b; ?>",
            CodeLanguage.PHP,
            "<p" to TAG,
            "echo" to KEYWORD,
            "\$b" to VARIABLE,
        )
    }

    @Test
    fun phpMarkupBeforeFirstTagAndAfterLastTag() {
        val head = "<p>Don't</p>\n<?php echo \$x; ?>"
        assertTokens(head, CodeLanguage.PHP, "<p" to TAG, "<?php" to META, "echo" to KEYWORD, "\$x" to VARIABLE)
        val tail = "<?php \$a = 1; ?>\n<p>it's</p>"
        assertTokens(tail, CodeLanguage.PHP, "<p" to TAG)
        assertTrue(tokens(tail, CodeLanguage.PHP).none { it.second == STRING && "</p>" in it.first })
        assertTokens("\$s = 'a';", CodeLanguage.PHP, "'a'" to STRING)
        val commented = "<?php endwhile; // End of the loop. ?>\n<p>Don't miss it</p>\n<?php get_footer(); ?>"
        assertTokens(commented, CodeLanguage.PHP, "// End of the loop. " to COMMENT, "<p" to TAG, "?>" to META)
        assertTrue(tokens(commented, CodeLanguage.PHP).none { it.second == STRING })
        assertTokens(
            "\$doc = new DOMDocument();\n\$doc->loadXML('<?xml version=\"1.0\"?><root/>');",
            CodeLanguage.PHP,
            "\$doc" to VARIABLE,
            "new" to KEYWORD,
            "'<?xml version=\"1.0\"?><root/>'" to STRING,
        )
    }

    @Test
    fun phpLineCommentsEndAtLineEnd() {
        assertTokens(
            "<?php\n// a\n\$x = 1;\n# b\necho \$x;\n?>\n<p>x</p>",
            CodeLanguage.PHP,
            "// a" to COMMENT,
            "# b" to COMMENT,
            "\$x" to VARIABLE,
            "echo" to KEYWORD,
            "<p" to TAG,
        )
    }

    @Test
    fun phpMarkupStartRequiresTagAndPhpTag() {
        assertTokens("<b>\necho \$x;", CodeLanguage.PHP, "echo" to KEYWORD, "\$x" to VARIABLE)
        val indented = "  <p>Don't</p>\n<?php echo \$x; ?>"
        assertTokens(indented, CodeLanguage.PHP, "<p" to TAG)
        assertTrue(tokens(indented, CodeLanguage.PHP).none { it.second == STRING && "</p>" in it.first })
        assertTokens("<p><?= \$name ?></p>", CodeLanguage.PHP, "<?=" to META, "\$name" to VARIABLE, "?>" to META, "</p" to TAG)
        assertTokens("<?PHP echo 1; ?>", CodeLanguage.PHP, "<?PHP" to META, "echo" to KEYWORD)
    }

    @Test
    fun kotlinHardKeywordsAndSoftNames() {
        assertTokens(
            "override suspend fun f() = when (x) { else -> null }",
            CodeLanguage.KOTLIN,
            "override" to KEYWORD,
            "suspend" to KEYWORD,
            "when" to KEYWORD,
            "else" to KEYWORD,
            "null" to NUMBER,
        )
        assertPlain(
            "x.set(1); o.property; r.receiver; d.delegate; v: dynamic; p.param",
            CodeLanguage.KOTLIN,
            "set",
            "property",
            "receiver",
            "delegate",
            "dynamic",
            "param",
        )
    }

    @Test
    fun yamlBlockKeepsBlankLines() {
        assertTokens(
            "script: |\n  echo a\n\n  echo it's\nnext: 1\n",
            CodeLanguage.YAML,
            "echo it's" to STRING,
            "next" to PROPERTY,
            "1" to NUMBER,
        )
    }

    @Test
    fun moreHeredocContexts() {
        assertTokens("x=\$((1+2)); cat <<EOF\nIt's done\nEOF\nexit 0", CodeLanguage.SHELL, "It's done\n" to STRING, "exit" to KEYWORD)
        assertTokens("list[0]<<x\nputs n", CodeLanguage.RUBY, "puts" to KEYWORD)
        assertTokens("foo()<<x\nputs n", CodeLanguage.RUBY, "puts" to KEYWORD)
        assertTokens("foo(<<-EOS)\n  it's\n  EOS\nputs 1", CodeLanguage.RUBY, "  it's\n" to STRING, "puts" to KEYWORD)
    }

    @Test
    fun embeddedScriptCommentStopsAtScriptEnd() {
        assertTokens(
            "<script>let a = 1 // c</script><p class=\"x\">y</p>",
            CodeLanguage.HTML,
            "// c" to COMMENT,
            "<p" to TAG,
            "class" to ATTRIBUTE,
        )
    }

    @Test
    fun markdownRuleForms() {
        assertTokens("* * *\n_ _ _", CodeLanguage.MARKDOWN, "* * *" to META, "_ _ _" to META)
        assertTrue(tokens("--", CodeLanguage.MARKDOWN).isEmpty())
        assertTokens("- item", CodeLanguage.MARKDOWN, "-" to KEYWORD)
    }

    @Test
    fun sqlIsCaseInsensitive() {
        assertTokens(
            "SELECT id FROM users WHERE name = 'O''Brien' -- c",
            CodeLanguage.SQL,
            "SELECT" to KEYWORD,
            "FROM" to KEYWORD,
            "'O''Brien'".substring(0, 3) to STRING,
            "-- c" to COMMENT,
        )
    }

    @Test
    fun groovyAndDart() {
        assertTokens("def v = \"\${x}\"", CodeLanguage.GROOVY, "def" to KEYWORD, "\${x}" to VARIABLE)
        assertTokens("final s = 'a \$b';", CodeLanguage.DART, "final" to KEYWORD, "\$b" to VARIABLE)
    }

    @Test
    fun jsonKeysAndValues() {
        assertTokens(
            "{\"name\": \"x\", \"n\": -1.5e3, \"ok\": true, \"z\": null}",
            CodeLanguage.JSON,
            "\"name\"" to PROPERTY,
            "\"x\"" to STRING,
            "-1.5e3" to NUMBER,
            "true" to NUMBER,
            "null" to NUMBER,
        )
    }

    @Test
    fun yamlKeysScalarsAndBlocks() {
        val code = "# c\nname: app\nlist:\n  - 1\n  - \"two\"\nscript: |\n  echo true\nflag: yes # on\n"
        assertTokens(
            code,
            CodeLanguage.YAML,
            "# c" to COMMENT,
            "name" to PROPERTY,
            "-" to KEYWORD,
            "1" to NUMBER,
            "\"two\"" to STRING,
            "|" to KEYWORD,
            "echo true" to STRING,
            "yes" to NUMBER,
            "# on" to COMMENT,
        )
    }

    @Test
    fun htmlEmbedsScriptAndStyle() {
        assertTokens(
            "<!-- c --><p class=\"a\">x &amp; y</p><script>let n = 1</script><style>p { color: red }</style>",
            CodeLanguage.HTML,
            "<!-- c -->" to COMMENT,
            "<p" to TAG,
            "class" to ATTRIBUTE,
            "\"a\"" to STRING,
            "&amp;" to META,
            "let" to KEYWORD,
            "color" to PROPERTY,
        )
    }

    @Test
    fun xmlTags() {
        assertTokens(
            "<?xml version=\"1.0\"?>\n<TextView android:id=\"@+id/t\"/>",
            CodeLanguage.XML,
            "<?xml version=\"1.0\"?>" to META,
            "<TextView" to TAG,
            "android:id" to ATTRIBUTE,
            "/>" to TAG,
        )
    }

    @Test
    fun cssSelectorsPropertiesAndValues() {
        assertTokens(
            "@media screen { .card:hover { color: #fff; margin: 1.5rem !important; --gap: 4px } }",
            CodeLanguage.CSS,
            "@media" to KEYWORD,
            ".card" to TYPE,
            "color" to PROPERTY,
            "#fff" to NUMBER,
            "1.5rem" to NUMBER,
            "!important" to KEYWORD,
            "--gap" to PROPERTY,
        )
    }

    @Test
    fun markdownStructure() {
        assertTokens(
            "# Title\n- item with `code`\n> quote\n```kotlin\nval a = 1\n```\n[link](https://example.com)",
            CodeLanguage.MARKDOWN,
            "# Title" to HEADING,
            "-" to KEYWORD,
            "`code`" to STRING,
            "> quote" to COMMENT,
            "```kotlin" to META,
            "val a = 1" to STRING,
            "(https://example.com)" to STRING,
        )
    }

    @Test
    fun diffLines() {
        assertTokens(
            "--- a/x\n+++ b/x\n@@ -1 +1 @@\n-old\n+new\n same",
            CodeLanguage.DIFF,
            "--- a/x" to META,
            "@@ -1 +1 @@" to KEYWORD,
            "-old" to DELETED,
            "+new" to INSERTED,
        )
    }

    @Test
    fun yamlBlockUnderSequenceItemEndsAtSiblingKey() {
        val code = "steps:\n  - run: |\n      ./gradlew build\n    shell: bash\n"
        assertTokens(code, CodeLanguage.YAML, "./gradlew build" to STRING, "shell" to PROPERTY)
    }

    @Test
    fun shellHeredocAndEscapedQuotesDoNotSwallowCode() {
        val code = "cat <<'EOF' > note.txt\nIt's done\nEOF\necho It\\'s \$HOME\nexit 0"
        assertTokens(
            code,
            CodeLanguage.SHELL,
            "<<'EOF'" to META,
            "It's done\n" to STRING,
            "EOF" to META,
            "\$HOME" to VARIABLE,
            "exit" to KEYWORD,
        )
    }

    @Test
    fun indentedHeredocsEndAtIndentedTerminator() {
        assertTokens("cat <<-EOF\n\ttext\n\tEOF\nexit 0", CodeLanguage.SHELL, "\ttext\n" to STRING, "exit" to KEYWORD)
        assertTokens("sql = <<~SQL\n  SELECT 1\n  SQL\nputs sql", CodeLanguage.RUBY, "  SELECT 1\n" to STRING, "puts" to KEYWORD)
    }

    @Test
    fun divisionAfterParenthesisAndRegexAfterReturn() {
        val code = "const m = (a + b) / 2 / n; return /x/.test(s)"
        assertTokens(code, CodeLanguage.JAVASCRIPT, "2" to NUMBER, "/x/" to STRING)
        assertTrue(tokens(code, CodeLanguage.JAVASCRIPT).none { it.first.contains("/ 2 /") })
    }

    @Test
    fun moreStringAndCommentForms() {
        assertTokens("String s = \"\"\"\n  if x\n  \"\"\";", CodeLanguage.JAVA, "\"\"\"\n  if x\n  \"\"\"" to STRING)
        assertTokens("#define A(x) \\\n  (x)\nint y;", CodeLanguage.C, "#define A(x) \\\n  (x)" to META, "int" to TYPE)
        assertTokens("=begin\nx\n=end\nputs 1", CodeLanguage.RUBY, "=begin\nx\n=end" to COMMENT, "puts" to KEYWORD)
        assertTokens("s = '''a\nb'''", CodeLanguage.PYTHON, "'''a\nb'''" to STRING)
        assertTokens("#if DEBUG\nlet a = 1", CodeLanguage.SWIFT, "#if" to META, "let" to KEYWORD)
        assertPlain("val `my name` = 1", CodeLanguage.KOTLIN, "my name")
        assertTokens("+++ b/x\n--- a/x", CodeLanguage.DIFF, "+++ b/x" to META)
        assertTokens("~~~\ncode\n~~~", CodeLanguage.MARKDOWN, "~~~" to META, "code" to STRING)
        assertTokens(
            "\"k\": &base !tag x\nother: *base\nfold: >\n  text",
            CodeLanguage.YAML,
            "\"k\"" to PROPERTY,
            "&base" to VARIABLE,
            "!tag" to META,
            "*base" to VARIABLE,
            ">" to KEYWORD,
            "text" to STRING,
        )
    }

    @Test
    fun aliasesResolveToTheirLanguages() {
        mapOf(
            "kt" to CodeLanguage.KOTLIN,
            "kts" to CodeLanguage.KOTLIN,
            "js" to CodeLanguage.JAVASCRIPT,
            "ts" to CodeLanguage.TYPESCRIPT,
            "sh" to CodeLanguage.SHELL,
            "yml" to CodeLanguage.YAML,
            "Kotlin" to CodeLanguage.KOTLIN,
        ).forEach { (name, language) -> assertEquals(language, CodeLanguage.resolve(name), name) }
    }

    @Test
    fun codeAtLimitIsHighlighted() {
        val code = "val a = 1\n".repeat(CodeHighlighter.MAX_CODE_LENGTH / 10)
        assertEquals(CodeHighlighter.MAX_CODE_LENGTH, code.length)
        assertTrue(CodeHighlighter.highlight(code, CodeLanguage.KOTLIN).isNotEmpty())
    }

    @Test
    fun shellHeredocForms() {
        listOf("cat<<EOF", "cat << EOF", "cat <<-EOF", "cat <<'EOF'", "cat << \"EOF\"").forEach { opener ->
            val code = "$opener\nIt's done\nEOF\nexit 0"
            assertTokens(code, CodeLanguage.SHELL, "It's done\n" to STRING, "exit" to KEYWORD)
        }
    }

    @Test
    fun shiftsAndAppendsAreNotHeredoc() {
        listOf("echo \$((1<<2))", "echo \$(( a << b ))", "tr a-z A-Z <<<hello").forEach { line ->
            assertTokens("$line\nexit 0", CodeLanguage.SHELL, "exit" to KEYWORD)
        }
        listOf("n = 1<<4", "class << self", "arr << x", "arr<<x", "x <<= 1").forEach { line ->
            assertTokens("$line\nputs n", CodeLanguage.RUBY, "puts" to KEYWORD)
        }
    }

    @Test
    fun longMarkdownRuleDoesNotOverflowStack() {
        assertTokens("-".repeat(LONG_RULE), CodeLanguage.MARKDOWN, "-".repeat(LONG_RULE) to META)
    }

    @Test
    fun codeOverLimitIsNotHighlighted() {
        val code = "val a = 1\n".repeat(CodeHighlighter.MAX_CODE_LENGTH / 10 + 1)
        assertEquals(emptyList(), CodeHighlighter.highlight(code, CodeLanguage.KOTLIN))
    }

    @Test
    fun plainTextHasNoSpans() {
        assertEquals(emptyList(), CodeHighlighter.highlight("val a = 1", CodeLanguage.TEXT))
    }

    @Test
    fun spansAreOrderedAndInsideTextForAnyInput() {
        val alphabet = "abcXYZ019 _\$@#'\"`/\\*-+.:;,!?()[]{}<>=&|%~^\n\t\r"
        val random = Random(SEED)
        CodeLanguage.entries.forEach { language ->
            repeat(SAMPLES) {
                val code = String(CharArray(random.nextInt(MAX_LENGTH)) { alphabet[random.nextInt(alphabet.length)] })
                var previousEnd = 0
                CodeHighlighter.highlight(code, language).forEach { span ->
                    assertTrue(span.start >= previousEnd && span.end > span.start && span.end <= code.length, "$language: $span в «$code»")
                    previousEnd = span.end
                }
            }
        }
    }

    private companion object {
        const val SEED = 7
        const val SAMPLES = 300
        const val MAX_LENGTH = 80
        const val LONG_RULE = 10_000
    }
}
