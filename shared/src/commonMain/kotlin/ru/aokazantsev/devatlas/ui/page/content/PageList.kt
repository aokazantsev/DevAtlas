package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.ui.page.code.CodeCopy

@Composable
internal fun PageList(
    list: PageBlock.ListBlock,
    diagrams: Map<String, String>,
    links: PageLinks,
    onOpenDiagram: (PageBlock.Figure) -> Unit,
    codeCopy: CodeCopy,
) {
    if (list.items.isEmpty()) return
    val style = AtlasTheme.typography.bodyLarge
    val labels = remember(list.items.size, list.ordered) {
        List(list.items.size) { index -> if (list.ordered) "${index + 1}." else BULLET }
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val markerWidthPx = remember(labels, style, measurer) { labels.maxOf { measurer.measure(it, style).size.width } }
    val markerWidth = with(density) { markerWidthPx.toDp() }
    Column(verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
        list.items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
                AtlasText(
                    text = labels[index],
                    style = style,
                    color = AtlasTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(markerWidth),
                )
                PageBlocks(item, diagrams, links, onOpenDiagram, codeCopy, Modifier.weight(1f))
            }
        }
    }
}

private const val BULLET = "•"
