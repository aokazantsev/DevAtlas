package ru.aokazantsev.devatlas.themeformat.svg

import kotlin.test.Test
import kotlin.test.assertEquals

class SvgValueRewriterTest {

    private fun upper(svg: String) = SvgValueRewriter.rewrite(svg) { it.uppercase() }

    @Test
    fun attributeValuesInBothQuoteStylesAreRewritten() {
        assertEquals("""<a x="AB" y='CD'>text</a>""", upper("""<a x="ab" y='cd'>text</a>"""))
    }

    @Test
    fun multilineAttributeValueIsRewrittenWhole() {
        assertEquals("<a style=\"FILL: A;\n  STROKE: B\"/>", upper("<a style=\"fill: a;\n  stroke: b\"/>"))
    }

    @Test
    fun textContentStaysAsIs() {
        assertEquals("""<text x="A">primary currentColor</text>""", upper("""<text x="a">primary currentColor</text>"""))
    }

    @Test
    fun styleElementContentIsRewrittenButScriptIsNot() {
        assertEquals("<style>.A { FILL: B }</style><script>let a</script>", upper("<style>.a { fill: b }</style><script>let a</script>"))
    }

    @Test
    fun emptyAndUnquotedValuesAreHandled() {
        assertEquals("""<a x="" y=Q/>""", upper("""<a x="" y=q/>"""))
    }

    @Test
    fun valuesAreRewrittenRawWithoutDecodingEntities() {
        assertEquals("""<a x="a&amp;b!">t &amp; t</a>""", SvgValueRewriter.rewrite("""<a x="a&amp;b">t &amp; t</a>""") { "$it!" })
    }
}
