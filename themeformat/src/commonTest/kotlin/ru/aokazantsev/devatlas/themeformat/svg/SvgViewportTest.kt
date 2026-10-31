package ru.aokazantsev.devatlas.themeformat.svg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SvgViewportTest {

    @Test
    fun viewBoxGivesRatio() {
        val viewport = SvgViewport.of("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="10 20 400, 100" width="1" height="1"/>""")

        assertEquals(SvgViewport(10f, 20f, 400f, 100f), viewport)
        assertEquals(4f, viewport?.ratio)
    }

    @Test
    fun prologAndCommentsBeforeRootAreSkipped() {
        val svg = """<?xml version="1.0"?><!-- <svg viewBox="0 0 1 9"> --><svg viewBox="0 0 300 150"><rect width="1" height="9"/></svg>"""

        assertEquals(2f, SvgViewport.of(svg)?.ratio)
    }

    @Test
    fun quotedGreaterThanInsideRootDoesNotCutTag() {
        assertEquals(3f, SvgViewport.of("""<svg aria-label="a > b" viewBox="0 0 30 10"/>""")?.ratio)
    }

    @Test
    fun widthAndHeightUsedWithoutViewBox() {
        assertEquals(SvgViewport(0f, 0f, 96f, 48f), SvgViewport.of("""<svg width="1in" height="48px"/>"""))
    }

    @Test
    fun defaultSizeWithoutViewBoxAndSize() {
        assertEquals(SvgViewport(0f, 0f, 300f, 150f), SvgViewport.of("""<svg width="100%" height="50"/>"""))
    }

    @Test
    fun brokenViewBoxFallsBackToSize() {
        assertEquals(SvgViewport(0f, 0f, 20f, 10f), SvgViewport.of("""<svg viewBox="0 0 0 10" width="20" height="10"/>"""))
    }

    @Test
    fun tallDiagramKeepsItsRatio() {
        assertEquals(0.1f, SvgViewport.of("""<svg viewBox="0 0 10 100"/>""")?.ratio)
    }

    @Test
    fun rootAfterLongPrologueIsFound() {
        val prologue = "<!-- " + "x".repeat(10_000) + " -->"

        assertEquals(2f, SvgViewport.of("""$prologue<svg viewBox="0 0 20 10"/>""")?.ratio)
    }

    @Test
    fun notSvgRootIsRejected() {
        assertNull(SvgViewport.of("""<html viewBox="0 0 1 1"/>"""))
        assertNull(SvgViewport.of("not markup"))
        assertNull(SvgViewport.of("""<svg viewBox="0 0 1 1" """))
    }
}
