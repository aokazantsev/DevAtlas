package ru.aokazantsev.devatlas.ui.page.code

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage

const val CODE_BLOCK_TAG = "code_block"

@Composable
fun CodeBlock(code: String, language: String?, onCopy: (String) -> Boolean, modifier: Modifier = Modifier) {
    val resolved = remember(language) { language?.let(CodeLanguage::resolve) }
    val palette = rememberCodePalette()
    val text = remember(code, resolved, palette) { highlightedCode(code, resolved, palette) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AtlasTheme.shapes.medium)
            .background(AtlasTheme.colors.surfaceContainerHigh)
            .testTag(CODE_BLOCK_TAG),
    ) {
        CodeHeader(label = resolved?.title ?: language.orEmpty(), onCopy = { onCopy(code) })
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = AtlasTheme.spacing.m, end = AtlasTheme.spacing.m, bottom = AtlasTheme.spacing.m),
        ) {
            AtlasText(text = text, style = AtlasTheme.typography.code)
        }
    }
}

@Composable
private fun CodeHeader(label: String, onCopy: () -> Boolean) {
    var feedback by remember { mutableStateOf<String?>(null) }
    var presses by remember { mutableIntStateOf(0) }
    LaunchedEffect(presses) {
        if (feedback != null) {
            delay(COPY_FEEDBACK_MILLIS)
            feedback = null
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = AtlasTheme.spacing.m, end = AtlasTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtlasText(
            text = label,
            style = AtlasTheme.typography.labelLarge,
            color = AtlasTheme.colors.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        AtlasTextButton(
            text = feedback ?: "Копировать",
            onClick = {
                feedback = if (onCopy()) "Скопировано" else "Не скопировано"
                presses++
            },
        )
    }
}

private const val COPY_FEEDBACK_MILLIS = 2_000L
