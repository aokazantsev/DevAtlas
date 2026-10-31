package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import ru.aokazantsev.devatlas.designsystem.component.AtlasDivider
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.ui.page.code.CodeBlock
import ru.aokazantsev.devatlas.ui.page.code.CodeCopy
import ru.aokazantsev.devatlas.ui.page.diagram.DiagramFigure
import ru.aokazantsev.devatlas.ui.scroll.ScrollAnchors

@Composable
internal fun PageBlocks(
    blocks: List<PageBlock>,
    diagrams: Map<String, String>,
    links: PageLinks,
    onOpenDiagram: (PageBlock.Figure) -> Unit,
    codeCopy: CodeCopy,
    modifier: Modifier = Modifier,
    anchors: ScrollAnchors? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m)) {
        blocks.forEachIndexed { index, block ->
            Box(modifier = anchors?.item(index) ?: Modifier) { PageBlockView(block, diagrams, links, onOpenDiagram, codeCopy) }
        }
    }
}

@Composable
private fun PageBlockView(
    block: PageBlock,
    diagrams: Map<String, String>,
    links: PageLinks,
    onOpenDiagram: (PageBlock.Figure) -> Unit,
    codeCopy: CodeCopy,
) {
    when (block) {
        is PageBlock.Heading -> InlineText(
            content = block.content,
            links = links,
            style = headingStyle(block.level),
            modifier = Modifier.semantics { heading() },
        )
        is PageBlock.Paragraph -> InlineText(
            content = block.content,
            links = links,
            style = AtlasTheme.typography.bodyLarge,
        )
        is PageBlock.ListBlock -> PageList(block, diagrams, links, onOpenDiagram, codeCopy)
        is PageBlock.Code -> CodeBlock(code = block.code, language = block.language, copy = codeCopy, inQuote = LocalInQuote.current)
        is PageBlock.Quote -> CompositionLocalProvider(LocalInQuote provides true) {
            PageBlocks(
                blocks = block.blocks,
                diagrams = diagrams,
                links = links,
                onOpenDiagram = onOpenDiagram,
                codeCopy = codeCopy,
                modifier = Modifier
                    .clip(AtlasTheme.shapes.medium)
                    .background(AtlasTheme.colors.surfaceContainerHigh)
                    .padding(AtlasTheme.spacing.m),
            )
        }
        is PageBlock.Table -> PageTable(rows = block.rows, links = links)
        is PageBlock.Figure -> DiagramFigure(
            figure = block,
            svg = diagrams[block.source],
            onOpen = { onOpenDiagram(block) },
            links = links,
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

private const val TOP_HEADING = 1
private const val SECTION_HEADING = 2
private const val SUBSECTION_HEADING = 3
