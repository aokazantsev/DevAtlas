package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasLoading(modifier: Modifier = Modifier, text: String? = null) {
    StateColumn(modifier) {
        LoadingIndicator(modifier = Modifier.size(AtlasTheme.sizes.loader))
        if (text != null) {
            AtlasText(text = text, color = AtlasTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(AtlasTheme.spacing.xl), contentAlignment = Alignment.Center) {
        LoadingIndicator(modifier = Modifier.size(AtlasTheme.sizes.loader))
    }
}

@Composable
fun AtlasError(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
    extraActions: @Composable () -> Unit = {},
) {
    StateColumn(modifier) {
        AtlasIcon(
            icon = AtlasIcons.Error,
            contentDescription = null,
            tint = AtlasTheme.colors.error,
            modifier = Modifier.size(AtlasTheme.sizes.iconLarge),
        )
        AtlasText(text = message, textAlign = TextAlign.Center)
        if (actionText != null) {
            AtlasSecondaryButton(text = actionText, onClick = onAction)
        }
        extraActions()
    }
}

@Composable
private fun StateColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(AtlasTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}
