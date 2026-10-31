package ru.aokazantsev.devatlas.update.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun tagsAndBuildNamesAreParsed() {
        assertEquals(AppVersion(listOf(0, 1)), AppVersion.parse("0.1-debug"))
        assertEquals(AppVersion(listOf(1, 2, 3)), AppVersion.parse("v1.2.3"))
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse(""))
    }

    @Test
    fun versionsCompareByNumbersNotText() {
        assertTrue(AppVersion.parse("0.10")!! > AppVersion.parse("0.9")!!)
        assertEquals(0, AppVersion.parse("1.0")!!.compareTo(AppVersion.parse("1")!!))
    }
}
