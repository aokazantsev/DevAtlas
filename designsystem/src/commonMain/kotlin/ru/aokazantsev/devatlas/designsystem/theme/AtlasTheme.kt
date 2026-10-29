package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAtlasColors = staticCompositionLocalOf { AtlasColors.Light }
private val LocalAtlasTypography = staticCompositionLocalOf { AtlasTypography.Default }
private val LocalAtlasSpacing = staticCompositionLocalOf { AtlasSpacing.Default }
private val LocalAtlasSizes = staticCompositionLocalOf { AtlasSizes.Default }
private val LocalAtlasShapes = staticCompositionLocalOf { AtlasShapes.Default }

object AtlasTheme {
    val colors: AtlasColors
        @Composable @ReadOnlyComposable
        get() = LocalAtlasColors.current

    val typography: AtlasTypography
        @Composable @ReadOnlyComposable
        get() = LocalAtlasTypography.current

    val spacing: AtlasSpacing
        @Composable @ReadOnlyComposable
        get() = LocalAtlasSpacing.current

    val sizes: AtlasSizes
        @Composable @ReadOnlyComposable
        get() = LocalAtlasSizes.current

    val shapes: AtlasShapes
        @Composable @ReadOnlyComposable
        get() = LocalAtlasShapes.current
}

@Composable
fun AtlasTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) AtlasColors.Dark else AtlasColors.Light
    CompositionLocalProvider(LocalAtlasColors provides colors) {
        MaterialTheme(colorScheme = colors.toColorScheme(darkTheme)) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
        }
    }
}

private fun AtlasColors.toColorScheme(darkTheme: Boolean) =
    (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
        primary = accent,
        onPrimary = onAccent,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = onSurfaceMuted,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        outline = divider,
        outlineVariant = divider,
        error = error,
        scrim = scrim,
    )
