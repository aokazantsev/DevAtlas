package ru.aokazantsev.devatlas.themeformat.code

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JsonSyntaxTest {

    @Test
    fun validDocumentsPass() {
        listOf(
            "{}",
            "[]",
            "\"x\"",
            "-0.5e+10",
            "{\"a\": [1, 2.5, true, false, null, {\"b\": \"\\u00e9\\n\"}], \"c\": {}}",
            "  [ ]  \n",
        ).forEach { assertNull(JsonSyntax.firstProblem(it), it) }
    }

    @Test
    fun problemsPointAtOffset() {
        assertEquals(8, JsonSyntax.firstProblem("{\"a\": 1,}")?.offset)
        assertEquals(1, JsonSyntax.firstProblem("{'a': 1}")?.offset)
        assertEquals(3, JsonSyntax.firstProblem("[1 2]")?.offset)
    }

    @Test
    fun closingBracketIsCalledExtraCommaOnlyAfterComma() {
        assertEquals("лишняя запятая перед «]»", JsonSyntax.firstProblem("[1, ]")?.message)
        assertEquals("лишняя запятая перед «]»", JsonSyntax.firstProblem("{\"a\": [1,\n\t]}")?.message)
        assertEquals("ожидалось значение, а стоит «}»", JsonSyntax.firstProblem("{\"a\": }")?.message)
        assertEquals("ожидалось значение, а стоит «]»", JsonSyntax.firstProblem("]")?.message)
    }

    @Test
    fun unclosedContainersAreReported() {
        assertEquals(JsonProblem(7, "ожидалась запятая или «}»"), JsonSyntax.firstProblem("{\"a\": 1"))
        assertEquals(JsonProblem(5, "ожидалась запятая или «]»"), JsonSyntax.firstProblem("[1, 2"))
    }

    @Test
    fun invalidDocumentsFail() {
        listOf(
            "",
            "{",
            "[1,]",
            "{\"a\" 1}",
            "{a: 1}",
            "01",
            "1.",
            "\"a\nb\"",
            "\"\\x\"",
            "\"\\u12\"",
            "True",
            "{} {}",
            "// c\n{}",
        ).forEach { assertNotNull(JsonSyntax.firstProblem(it), it) }
    }

    @Test
    fun deepNestingDoesNotOverflowStack() {
        val depth = 100_000
        assertNull(JsonSyntax.firstProblem("[".repeat(depth) + "]".repeat(depth)))
    }
}
