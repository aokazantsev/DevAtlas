package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasLoading(modifier: Modifier = Modifier, text: String? = null) {
    val colors = AtlasTheme.colors
    StateColumn(modifier) {
        CircularProgressIndicator(
            modifier = Modifier.size(AtlasTheme.sizes.loader),
            color = colors.accent,
            trackColor = colors.surfaceRaised,
            strokeWidth = AtlasTheme.sizes.loaderStroke,
        )
        if (text != null) {
            AtlasText(text = text, color = colors.onSurfaceMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AtlasError(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = AtlasTheme.colors
    StateColumn(modifier) {
        AtlasIcon(
            icon = AtlasIcons.Error,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(AtlasTheme.sizes.iconLarge),
        )
        AtlasText(text = message, textAlign = TextAlign.Center)
        if (actionText != null) {
            AtlasSecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
private fun StateColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AtlasTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
