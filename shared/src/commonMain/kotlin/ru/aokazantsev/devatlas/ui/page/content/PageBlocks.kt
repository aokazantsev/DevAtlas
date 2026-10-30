package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import ru.aokazantsev.devatlas.designsystem.component.AtlasDivider
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.ui.page.code.CodeBlock
import ru.aokazantsev.devatlas.ui.page.diagram.DiagramFigure
import ru.aokazantsev.devatlas.ui.scroll.ScrollAnchors

@Composable
internal fun PageBlocks(
    blocks: List<PageBlock>,
    diagrams: Map<String, String>,
    onLink: (LinkTarget) -> Unit,
    onOpenDiagram: (PageBlock.Figure) -> Unit,
    modifier: Modifier = Modifier,
    anchors: ScrollAnchors? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m)) {
        blocks.forEachIndexed { index, block ->
            Box(modifier = anchors?.item(index) ?: Modifier) { PageBlockView(block, diagrams, onLink, onOpenDiagram) }
        }
    }
}

@Composable
private fun PageBlockView(
    block: PageBlock,
    diagrams: Map<String, String>,
    onLink: (LinkTarget) -> Unit,
    onOpenDiagram: (PageBlock.Figure) -> Unit,
) {
    when (block) {
        is PageBlock.Heading -> AtlasText(
            text = rememberInlineText(block.content, onLink),
            style = headingStyle(block.level),
            modifier = Modifier.semantics { heading() },
        )
        is PageBlock.Paragraph -> AtlasText(
            text = rememberInlineText(block.content, onLink),
            style = AtlasTheme.typography.bodyLarge,
        )
        is PageBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
            block.items.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
                    AtlasText(
                        text = if (block.ordered) "${index + 1}." else BULLET,
                        style = AtlasTheme.typography.bodyLarge,
                        color = AtlasTheme.colors.onSurfaceVariant,
                    )
                    PageBlocks(item, diagrams, onLink, onOpenDiagram, Modifier.weight(1f))
                }
            }
        }
        is PageBlock.Code -> CodeBlock(code = block.code)
        is PageBlock.Quote -> PageBlocks(
            blocks = block.blocks,
            diagrams = diagrams,
            onLink = onLink,
            onOpenDiagram = onOpenDiagram,
            modifier = Modifier
                .clip(AtlasTheme.shapes.medium)
                .background(AtlasTheme.colors.surfaceContainerHigh)
                .padding(AtlasTheme.spacing.m),
        )
        is PageBlock.Table -> PageTable(rows = block.rows, onLink = onLink)
        is PageBlock.Figure -> DiagramFigure(
            figure = block,
            svg = diagrams[block.source],
            onOpen = { onOpenDiagram(block) },
            onLink = onLink,
        )
        PageBlock.Divider -> AtlasDivider(modifier = Modifier.padding(vertical = AtlasTheme.spacing.s))
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    TOP_HEADING -> AtlasTheme.typography.headlineSmall
    SECTION_HEADING -> AtlasTheme.typography.titleLarge
    SUBSECTION_HEADING -> AtlasTheme.typography.titleMedium
    else -> AtlasTheme.typography.titleSmall
}

private const val BULLET = "•"
private const val TOP_HEADING = 1
private const val SECTION_HEADING = 2
private const val SUBSECTION_HEADING = 3
