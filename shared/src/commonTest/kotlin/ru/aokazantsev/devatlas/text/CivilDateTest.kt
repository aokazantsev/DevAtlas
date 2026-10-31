package ru.aokazantsev.devatlas.text

import kotlin.test.Test
import kotlin.test.assertEquals

class CivilDateTest {

    @Test
    fun epochStartIsFirstOfJanuary1970() {
        assertEquals(CivilDate(1970, 1, 1), CivilDate.ofEpochMillis(0L))
    }

    @Test
    fun leapDayAndYearEndAreExact() {
        assertEquals(CivilDate(2024, 2, 29), CivilDate.ofEpochMillis(1_709_164_800_000L))
        assertEquals(CivilDate(2026, 12, 31), CivilDate.ofEpochMillis(1_798_761_599_000L))
    }

    @Test
    fun negativeMillisGoBeforeEpoch() {
        assertEquals(CivilDate(1969, 12, 31), CivilDate.ofEpochMillis(-1L))
    }

    @Test
    fun formatsHaveTwoDigitDayAndMonth() {
        assertEquals("2026-03-05", CivilDate(2026, 3, 5).iso())
        assertEquals("05.03.2026", CivilDate(2026, 3, 5).russian())
    }
}
