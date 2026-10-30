package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgAnimationTest {

    private fun svg(body: String) = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\" fill=\"currentColor\">$body</svg>"

    private fun codes(source: String) = SvgChecker.check("diagrams/a.svg", source).map { it.code }

    @Test
    fun smilElementsAreForbidden() {
        listOf(
            "<rect><set attributeName=\"fill\" to=\"red\"/></rect>",
            "<rect><animate attributeName=\"fill\" values=\"red;blue\" dur=\"1s\"/></rect>",
            "<rect><animateTransform attributeName=\"transform\" type=\"rotate\" to=\"90\"/></rect>",
            "<rect><animateMotion path=\"M0,0 L1,1\"/></rect>",
            "<rect><animateColor attributeName=\"fill\" to=\"red\"/></rect>",
        ).forEach { body ->
            assertEquals(listOf(IssueCode.SVG_FORBIDDEN), codes(svg(body)), body)
        }
    }

    @Test
    fun animationBypassingColorCheckIsReportedOnce() {
        val issues = SvgChecker.check("a.svg", svg("<rect><set attributeName=\"fill\" to=\"#ff0000\"/></rect>"))

        assertEquals(1, issues.size)
        assertTrue("анимация" in issues.single().message)
    }

    @Test
    fun prefixedAnimationIsForbiddenToo() {
        assertEquals(listOf(IssueCode.SVG_FORBIDDEN), codes(svg("<rect><svg:set attributeName=\"fill\" to=\"red\"/></rect>")))
    }
}
