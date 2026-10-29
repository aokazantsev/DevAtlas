package ru.aokazantsev.devatlas.settings.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {

    @Test
    fun explicitModesIgnoreSystem() {
        assertEquals(false, ThemeMode.LIGHT.isDark(systemIsDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemIsDark = false))
    }

    @Test
    fun systemModeFollowsSystem() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemIsDark = false))
    }
}
