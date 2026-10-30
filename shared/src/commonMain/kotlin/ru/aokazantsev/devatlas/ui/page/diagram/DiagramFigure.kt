package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.ui.page.content.rememberInlineText

@Composable
internal fun DiagramFigure(
    figure: PageBlock.Figure,
    svg: String?,
    onOpen: () -> Unit,
    onLink: (LinkTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
        val palette = rememberSvgPalette()
        val themed = remember(svg, palette) { svg?.let { SvgColorTokens.resolve(it, palette.roles, palette.textColor) } }
        val frame = Modifier
            .fillMaxWidth()
            .clip(AtlasTheme.shapes.medium)
            .background(AtlasTheme.colors.surface)
        if (themed == null) {
            DiagramUnavailable(frame)
        } else {
            SvgImage(
                svg = themed,
                contentDescription = figure.alt ?: "Схема",
                modifier = frame
                    .clickable(onClickLabel = "Открыть на весь экран", role = Role.Button, onClick = onOpen)
                    .padding(AtlasTheme.spacing.s),
                fallback = { DiagramUnavailable(frame) },
            )
        }
        if (figure.caption.isNotEmpty()) {
            AtlasText(
                text = rememberInlineText(figure.caption, onLink),
                style = AtlasTheme.typography.bodySmall,
                color = AtlasTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun DiagramUnavailable(modifier: Modifier = Modifier) {
    AtlasText(
        text = "Схема не открывается",
        color = AtlasTheme.colors.onSurfaceVariant,
        modifier = modifier.padding(AtlasTheme.spacing.l),
    )
}
