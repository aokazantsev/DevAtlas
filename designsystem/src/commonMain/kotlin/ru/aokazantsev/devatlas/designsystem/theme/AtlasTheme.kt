package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAtlasTypography = staticCompositionLocalOf { AtlasTypography.from(Typography()) }
private val LocalAtlasSpacing = staticCompositionLocalOf { AtlasSpacing.Default }
private val LocalAtlasSizes = staticCompositionLocalOf { AtlasSizes.Default }
private val LocalAtlasShapes = staticCompositionLocalOf<AtlasShapes> { error("AtlasShapes доступны только внутри AtlasTheme") }

object AtlasTheme {
    val colors: ColorScheme
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

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

    val wallpaperColorsAvailable: Boolean
        get() = isWallpaperColorSchemeAvailable()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasTheme(
    darkTheme: Boolean,
    wallpaperColors: Boolean,
    content: @Composable () -> Unit,
) {
    val wallpaperScheme = if (wallpaperColors) rememberWallpaperColorScheme(darkTheme) else null
    val colorScheme = wallpaperScheme ?: AtlasSeedScheme.rememberColorScheme(darkTheme)
    val typography = remember { Typography() }
    val atlasTypography = remember(typography) { AtlasTypography.from(typography) }
    val atlasShapes = AtlasShapes.default()
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
    ) {
        CompositionLocalProvider(
            LocalAtlasTypography provides atlasTypography,
            LocalAtlasShapes provides atlasShapes,
            content = content,
        )
    }
}
