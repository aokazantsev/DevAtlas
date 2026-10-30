package ru.aokazantsev.devatlas.themeformat.svg

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgRenderBudgetTest {

    private fun svg(body: String) = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">$body</svg>"""

    @Test
    fun ordinaryDiagramFits() {
        assertTrue(SvgRenderBudget.fits(svg("""<defs><g id="a"><rect/></g></defs><use href="#a"/><use xlink:href="#a"/>""")))
    }

    @Test
    fun useFanOutOverBudgetDoesNotFit() {
        val levels = buildString {
            append("""<g id="l0"><rect/></g>""")
            for (level in 1..6) {
                append("""<g id="l$level">""")
                repeat(10) { append("""<use href="#l${level - 1}"/>""") }
                append("</g>")
            }
        }

        assertFalse(SvgRenderBudget.fits(svg(levels)))
    }

    @Test
    fun useCycleDoesNotFit() {
        assertFalse(SvgRenderBudget.fits(svg("""<g id="a"><use href="#b"/></g><g id="b"><use href="#a"/></g>""")))
    }

    @Test
    fun anyLinkPrefixAndXmlIdAreFollowed() {
        assertFalse(SvgRenderBudget.fits(svg("""<g xml:id="a"><use l:href="#a"/></g>""")))
    }

    @Test
    fun selfReferenceDoesNotFit() {
        assertFalse(SvgRenderBudget.fits(svg("""<g id="a"><use href="#a"/></g>""")))
    }

    @Test
    fun longReferenceChainDoesNotFit() {
        val chain = buildString {
            append("""<g id="u0"><rect/></g>""")
            for (index in 1..40) append("""<g id="u$index"><use href="#u${index - 1}"/></g>""")
        }

        assertFalse(SvgRenderBudget.fits(svg(chain)))
    }

    @Test
    fun chainOfMaximumDepthFits() {
        val chain = buildString {
            append("""<g id="u0"><rect/></g>""")
            for (index in 1..32) append("""<g id="u$index"><use href="#u${index - 1}"/></g>""")
        }

        assertTrue(SvgRenderBudget.fits(svg(chain)))
    }

    @Test
    fun nestedReuseUnderBudgetFits() {
        val levels = buildString {
            append("""<symbol id="l0"><rect/></symbol>""")
            for (level in 1..3) {
                append("""<symbol id="l$level">""")
                repeat(10) { append("""<use href="#l${level - 1}"/>""") }
                append("</symbol>")
            }
            append("""<use href="#l3"/>""")
        }

        assertTrue(SvgRenderBudget.fits(svg(levels)))
    }

    @Test
    fun exactlyBudgetFits() {
        assertTrue(SvgRenderBudget.fits(svg("<rect/>".repeat(19_999))))
    }

    @Test
    fun tooManyPlainElementsDoNotFit() {
        assertFalse(SvgRenderBudget.fits(svg("<rect/>".repeat(20_001))))
    }

    @Test
    fun missingAndExternalTargetsAreIgnored() {
        assertTrue(SvgRenderBudget.fits(svg("""<use href="#nope"/><use href="other.svg#a"/>""")))
    }
}
