package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ru.aokazantsev.devatlas.designsystem.theme.AtlasColorRoles
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
internal fun rememberSvgPalette(): SvgPalette {
    val colors = AtlasTheme.colors
    return remember(colors) {
        SvgPalette(
            roles = AtlasColorRoles.colors(colors).mapValues { (_, color) -> hex(color) },
            textColor = hex(colors.onSurface),
        )
    }
}

internal fun hex(color: Color): String = "#" + (color.toArgb() and RGB_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS, '0')

private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS = 6
