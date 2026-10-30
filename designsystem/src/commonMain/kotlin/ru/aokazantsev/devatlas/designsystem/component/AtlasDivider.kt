package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = AtlasTheme.colors.outlineVariant)
}
