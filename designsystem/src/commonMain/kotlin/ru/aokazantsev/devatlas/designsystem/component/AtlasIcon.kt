package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AtlasTheme.colors.onSurface,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(AtlasTheme.sizes.icon),
        tint = tint,
    )
}

@Composable
fun AtlasIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AtlasTheme.colors.onSurface,
) {
    IconButton(onClick = onClick, modifier = modifier.size(AtlasTheme.sizes.touchTarget)) {
        AtlasIcon(icon = icon, contentDescription = contentDescription, tint = tint)
    }
}
