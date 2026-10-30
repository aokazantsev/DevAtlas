package ru.aokazantsev.devatlas.ui.page.code

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AtlasTheme.shapes.medium)
            .background(AtlasTheme.colors.surfaceContainerHigh)
            .horizontalScroll(rememberScrollState())
            .padding(AtlasTheme.spacing.m),
    ) {
        AtlasText(text = code.trimEnd('\n', '\r'), style = AtlasTheme.typography.code)
    }
}
