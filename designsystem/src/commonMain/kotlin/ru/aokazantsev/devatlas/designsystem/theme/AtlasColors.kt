package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
class AtlasColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val divider: Color,
    val success: Color,
    val error: Color,
    val scrim: Color,
) {
    companion object {
        val Light = AtlasColors(
            background = Color(0xFFF4F4F5),
            surface = Color(0xFFFFFFFF),
            surfaceRaised = Color(0xFFE9E9EC),
            onSurface = Color(0xFF18181B),
            onSurfaceMuted = Color(0xFF6B6B73),
            accent = Color(0xFF2F6FEB),
            onAccent = Color(0xFFFFFFFF),
            divider = Color(0xFFDCDCE0),
            success = Color(0xFF2F6FEB),
            error = Color(0xFF18181B),
            scrim = Color(0x99000000),
        )

        val Dark = AtlasColors(
            background = Color(0xFF111113),
            surface = Color(0xFF1B1B1E),
            surfaceRaised = Color(0xFF26262A),
            onSurface = Color(0xFFEDEDEF),
            onSurfaceMuted = Color(0xFFA1A1AA),
            accent = Color(0xFF7BA4FF),
            onAccent = Color(0xFF0B1630),
            divider = Color(0xFF303035),
            success = Color(0xFF7BA4FF),
            error = Color(0xFFEDEDEF),
            scrim = Color(0x99000000),
        )
    }
}
