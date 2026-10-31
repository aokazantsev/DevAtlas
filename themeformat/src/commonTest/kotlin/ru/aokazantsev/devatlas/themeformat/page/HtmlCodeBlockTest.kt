package ru.aokazantsev.devatlas.themeformat.page

import ru.aokazantsev.devatlas.themeformat.code.CodeHighlighter
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlCodeBlockTest {

    private fun issues(html: String) = HtmlPageParser.parse("pages/page.html", html).issues

    @Test
    fun knownLanguagesAndAliasesPassSilently() {
        (CodeLanguage.entries.flatMap { it.aliases + it.id } + "KOTLIN").forEach { name ->
            val found = issues("<pre><code class=\"language-$name\">x</code></pre>").filter { it.code == IssueCode.CODE_LANGUAGE }
            assertEquals(emptyList(), found, name)
        }
    }

    @Test
    fun languageClassIsFoundAmongAnyWhitespaceSeparators() {
        val classes = listOf(
            "a\tlanguage-kotlin", "a\nlanguage-kotlin", "language-kotlin\r\nb", "  language-kotlin  ", "x\u000Clanguage-kotlin",
        )
        classes.forEach { cls ->
            val page = HtmlPageParser.parse("pages/page.html", "<pre><code class=\"$cls\">x</code></pre>")
            val block = page.document.blocks.single() as PageBlock.Code
            assertEquals("kotlin", block.language, cls)
            assertEquals(emptyList(), page.issues.filter { it.code == IssueCode.CODE_LANGUAGE }, cls)
        }
    }

    @Test
    fun missingOrUnknownLanguageIsWarning() {
        listOf(
            "<pre><code>val a = 1</code></pre>",
            "<pre>val a = 1</pre>",
            "<pre><code class=\"kotlin\">val a = 1</code></pre>",
            "<pre><code class=\"language-lua\">x = 1</code></pre>",
        ).forEach { html ->
            val issue = issues(html).single()
            assertEquals(IssueCode.CODE_LANGUAGE, issue.code, html)
            assertEquals(IssueSeverity.WARNING, issue.severity)
            assertEquals(1, issue.line)
        }
    }

    @Test
    fun tagDirectlyInsidePreIsErrorEvenWithoutCode() {
        assertTrue(IssueCode.CODE_MARKUP in issues("<pre>a<br>b</pre>").map { it.code })
    }

    @Test
    fun invalidJsonIsWarningWithPlaceInBlock() {
        val issue = issues("<p>x</p>\n<pre><code class=\"language-json\">{\n  \"a\": 1,\n}</code></pre>").single()
        assertEquals(IssueCode.CODE_JSON_INVALID, issue.code)
        assertEquals(IssueSeverity.WARNING, issue.severity)
        assertEquals(2, issue.line)
        assertTrue("строка 3, столбец 1" in issue.message, issue.message)
    }

    @Test
    fun codeOverHighlightLimitIsWarnedExceptPlainText() {
        val long = "a".repeat(CodeHighlighter.MAX_CODE_LENGTH + 1)
        assertEquals(
            listOf(IssueCode.CODE_TOO_LONG),
            issues("<pre><code class=\"language-kotlin\">$long</code></pre>").map { it.code },
        )
        assertTrue(issues("<pre><code class=\"language-text\">$long</code></pre>").isEmpty())
        val atLimit = "a".repeat(CodeHighlighter.MAX_CODE_LENGTH)
        assertTrue(issues("<pre><code class=\"language-kotlin\">$atLimit</code></pre>").isEmpty())
    }

    @Test
    fun validJsonWithEntitiesPasses() {
        assertTrue(issues("<pre><code class=\"language-json\">{\"a\": \"&lt;b&gt; &amp;\"}</code></pre>").isEmpty())
    }
}
