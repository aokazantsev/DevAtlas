package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AtlasTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = AtlasTheme.sizes.buttonHeight),
        enabled = enabled,
        shape = AtlasTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.surfaceRaised,
            disabledContentColor = colors.onSurfaceMuted,
        ),
        contentPadding = buttonPadding(),
    ) {
        AtlasText(text = text, style = AtlasTheme.typography.label, color = if (enabled) colors.onAccent else colors.onSurfaceMuted)
    }
}

@Composable
fun AtlasSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AtlasTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AtlasTheme.sizes.buttonHeight),
        enabled = enabled,
        shape = AtlasTheme.shapes.medium,
        border = BorderStroke(AtlasTheme.sizes.divider, colors.divider),
        contentPadding = buttonPadding(),
    ) {
        AtlasText(text = text, style = AtlasTheme.typography.label, color = if (enabled) colors.onSurface else colors.onSurfaceMuted)
    }
}

@Composable
fun AtlasTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AtlasTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AtlasTheme.sizes.buttonHeight),
        enabled = enabled,
        shape = AtlasTheme.shapes.medium,
        contentPadding = buttonPadding(),
    ) {
        AtlasText(text = text, style = AtlasTheme.typography.label, color = if (enabled) colors.accent else colors.onSurfaceMuted)
    }
}

@Composable
private fun buttonPadding() = PaddingValues(horizontal = AtlasTheme.spacing.l, vertical = AtlasTheme.spacing.s)
