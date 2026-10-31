package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoadingIndicator
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.ui.page.content.InlineText
import ru.aokazantsev.devatlas.ui.page.content.PageLinks

@Composable
internal fun DiagramFigure(
    figure: PageBlock.Figure,
    svg: String?,
    onOpen: () -> Unit,
    links: PageLinks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
        val palette = rememberSvgPalette()
        val frame = Modifier
            .fillMaxWidth()
            .clip(AtlasTheme.shapes.medium)
            .background(AtlasTheme.colors.surface)
        if (svg == null) {
            DiagramUnavailable(frame)
        } else {
            SvgImage(
                svg = svg,
                palette = palette,
                contentDescription = figure.alt ?: "Схема",
                modifier = frame
                    .clickable(onClickLabel = "Открыть на весь экран", role = Role.Button, onClick = onOpen)
                    .padding(AtlasTheme.spacing.s),
                loading = { sized -> AtlasLoadingIndicator(frame.padding(AtlasTheme.spacing.s).then(sized)) },
                fallback = { sized -> DiagramUnavailable(frame.padding(AtlasTheme.spacing.s).then(sized)) },
            )
        }
        if (figure.caption.isNotEmpty()) {
            InlineText(
                content = figure.caption,
                links = links,
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
