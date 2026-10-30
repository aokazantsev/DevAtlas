package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

internal object AtlasSeedScheme {

    private val Seed = Color(0xFF2F6FEB)

    @Composable
    fun rememberColorScheme(darkTheme: Boolean): ColorScheme =
        rememberDynamicColorScheme(
            seedColor = Seed,
            isDark = darkTheme,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
}
