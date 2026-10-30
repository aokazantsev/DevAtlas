package ru.aokazantsev.devatlas.themeformat.path

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemePathsTest {

    @Test
    fun segmentEndingWithDotOrSpaceIsNotPlain() {
        assertFalse(ThemePaths.isPlainRelative("pages /intro.html"))
        assertTrue(ThemePaths.isPlainRelative("pages/intro.html"))
        assertFalse(ThemePaths.isPlainRelative("pages./a.html"))
        assertFalse(ThemePaths.isPlainRelative("pages/a "))
        assertFalse(ThemePaths.isPlainRelative("pages/a."))
    }

    @Test
    fun dotsInTheMiddleAndExtensionsStayValid() {
        assertTrue(ThemePaths.isPlainRelative("pages/v1.2/a.b.html"))
        assertTrue(ThemePaths.isPlainRelative(".hidden/a.html"))
    }

    @Test
    fun resolveRejectsSegmentsThatBreakOnWindows() {
        assertNull(ThemePaths.resolve("pages/a.html", "../diagrams /x.svg"))
        assertNull(ThemePaths.resolve("pages/a.html", "x./y.svg"))
        assertEquals("diagrams/x.svg", ThemePaths.resolve("pages/a.html", "../diagrams/x.svg"))
        assertEquals("pages/x.svg", ThemePaths.resolve("pages/a.html", "./x.svg"))
    }
}
