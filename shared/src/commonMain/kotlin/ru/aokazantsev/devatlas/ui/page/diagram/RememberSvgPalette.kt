package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.svg.ColorRole

@Composable
internal fun rememberSvgPalette(): SvgPalette {
    val c = AtlasTheme.colors
    return remember(c) {
        val roleColors = mapOf(
            ColorRole.PRIMARY to c.primary,
            ColorRole.ON_PRIMARY to c.onPrimary,
            ColorRole.PRIMARY_CONTAINER to c.primaryContainer,
            ColorRole.ON_PRIMARY_CONTAINER to c.onPrimaryContainer,
            ColorRole.INVERSE_PRIMARY to c.inversePrimary,
            ColorRole.SECONDARY to c.secondary,
            ColorRole.ON_SECONDARY to c.onSecondary,
            ColorRole.SECONDARY_CONTAINER to c.secondaryContainer,
            ColorRole.ON_SECONDARY_CONTAINER to c.onSecondaryContainer,
            ColorRole.TERTIARY to c.tertiary,
            ColorRole.ON_TERTIARY to c.onTertiary,
            ColorRole.TERTIARY_CONTAINER to c.tertiaryContainer,
            ColorRole.ON_TERTIARY_CONTAINER to c.onTertiaryContainer,
            ColorRole.BACKGROUND to c.background,
            ColorRole.ON_BACKGROUND to c.onBackground,
            ColorRole.SURFACE to c.surface,
            ColorRole.ON_SURFACE to c.onSurface,
            ColorRole.SURFACE_VARIANT to c.surfaceVariant,
            ColorRole.ON_SURFACE_VARIANT to c.onSurfaceVariant,
            ColorRole.SURFACE_TINT to c.surfaceTint,
            ColorRole.INVERSE_SURFACE to c.inverseSurface,
            ColorRole.INVERSE_ON_SURFACE to c.inverseOnSurface,
            ColorRole.ERROR to c.error,
            ColorRole.ON_ERROR to c.onError,
            ColorRole.ERROR_CONTAINER to c.errorContainer,
            ColorRole.ON_ERROR_CONTAINER to c.onErrorContainer,
            ColorRole.OUTLINE to c.outline,
            ColorRole.OUTLINE_VARIANT to c.outlineVariant,
            ColorRole.SCRIM to c.scrim,
            ColorRole.SURFACE_BRIGHT to c.surfaceBright,
            ColorRole.SURFACE_DIM to c.surfaceDim,
            ColorRole.SURFACE_CONTAINER to c.surfaceContainer,
            ColorRole.SURFACE_CONTAINER_HIGH to c.surfaceContainerHigh,
            ColorRole.SURFACE_CONTAINER_HIGHEST to c.surfaceContainerHighest,
            ColorRole.SURFACE_CONTAINER_LOW to c.surfaceContainerLow,
            ColorRole.SURFACE_CONTAINER_LOWEST to c.surfaceContainerLowest,
            ColorRole.PRIMARY_FIXED to c.primaryFixed,
            ColorRole.PRIMARY_FIXED_DIM to c.primaryFixedDim,
            ColorRole.ON_PRIMARY_FIXED to c.onPrimaryFixed,
            ColorRole.ON_PRIMARY_FIXED_VARIANT to c.onPrimaryFixedVariant,
            ColorRole.SECONDARY_FIXED to c.secondaryFixed,
            ColorRole.SECONDARY_FIXED_DIM to c.secondaryFixedDim,
            ColorRole.ON_SECONDARY_FIXED to c.onSecondaryFixed,
            ColorRole.ON_SECONDARY_FIXED_VARIANT to c.onSecondaryFixedVariant,
            ColorRole.TERTIARY_FIXED to c.tertiaryFixed,
            ColorRole.TERTIARY_FIXED_DIM to c.tertiaryFixedDim,
            ColorRole.ON_TERTIARY_FIXED to c.onTertiaryFixed,
            ColorRole.ON_TERTIARY_FIXED_VARIANT to c.onTertiaryFixedVariant,
        )
        SvgPalette(
            roles = ColorRole.entries.associate { role -> role.cssName to hex(roleColors.getValue(role)) },
            textColor = hex(c.onSurface),
        )
    }
}

private fun hex(color: Color): String = "#" + (color.toArgb() and RGB_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS, '0')

private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS = 6
