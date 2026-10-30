package ru.aokazantsev.devatlas.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSwitchRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.ui.page.content.PageBlocks
import ru.aokazantsev.devatlas.ui.page.diagram.DiagramViewer
import ru.aokazantsev.devatlas.ui.page.diagram.SvgColorTokens
import ru.aokazantsev.devatlas.ui.page.diagram.rememberSvgPalette
import ru.aokazantsev.devatlas.ui.scroll.RestoreAnchor
import ru.aokazantsev.devatlas.ui.scroll.ScrollAnchors

@Composable
fun PageScreen(
    viewModel: PageViewModel,
    anchor: Int,
    onOpenPage: (String) -> Unit,
    onNext: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = when (val shown = state) {
        is PageState.Ready -> shown.title
        is PageState.Unreadable -> shown.title
        else -> null
    } ?: "Страница"
    AtlasScaffold(topBar = { AtlasTopBar(title = title, onBack = onBack) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val shown = state) {
            PageState.Loading -> AtlasLoading(modifier = modifier)
            PageState.Missing -> AtlasError(
                message = "Этой страницы нет в текущей теме",
                actionText = "Назад",
                onAction = onBack,
                modifier = modifier,
            )
            is PageState.Unreadable -> AtlasError(
                message = "Страница не открывается: файл темы повреждён. Перезапустите приложение — оно предложит скачать тему заново.",
                actionText = "Назад",
                onAction = onBack,
                modifier = modifier,
            )
            is PageState.Ready -> PageBody(
                state = shown,
                anchor = anchor,
                onViewedChange = viewModel::setViewed,
                onOpenPage = onOpenPage,
                onNext = onNext,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PageBody(
    state: PageState.Ready,
    anchor: Int,
    onViewedChange: (Boolean) -> Unit,
    onOpenPage: (String) -> Unit,
    onNext: (String) -> Unit,
    modifier: Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val onLink = remember(onOpenPage, uriHandler) {
        { target: LinkTarget ->
            when (target) {
                is LinkTarget.Page -> onOpenPage(target.id)
                is LinkTarget.Web -> uriHandler.openSafely(target.url)
                is LinkTarget.Email -> uriHandler.openSafely(target.uri)
                is LinkTarget.Phone -> uriHandler.openSafely(target.uri)
            }
        }
    }
    var openedDiagram by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val anchors = remember { ScrollAnchors() }
    RestoreAnchor(anchors, scrollState, anchor)
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .then(anchors.contentModifier)
            .padding(vertical = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.xl),
    ) {
        PageBlocks(
            blocks = state.content.document.blocks,
            diagrams = state.content.diagrams,
            onLink = onLink,
            onOpenDiagram = { openedDiagram = it.source },
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            anchors = anchors,
        )
        AtlasListGroup {
            AtlasSwitchRow(title = "Просмотрено", checked = state.viewed, onCheckedChange = onViewedChange)
        }
        state.next?.let { next -> NextPageButton(next, onNext) }
    }
    val diagramSource = openedDiagram ?: return
    val diagramSvg = state.content.diagrams[diagramSource] ?: return
    val palette = rememberSvgPalette()
    val themed = remember(diagramSvg, palette) { SvgColorTokens.resolve(diagramSvg, palette.roles, palette.textColor) }
    DiagramViewer(
        svg = themed,
        contentDescription = state.content.document.blocks.figureAlt(diagramSource),
        onClose = { openedDiagram = null },
    )
}

@Composable
private fun NextPageButton(next: NextPage, onNext: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasText(
            text = "Следующая: ${next.title}",
            style = AtlasTheme.typography.bodyMedium,
            color = AtlasTheme.colors.onSurfaceVariant,
        )
        AtlasPrimaryButton(text = "Дальше", onClick = { onNext(next.id) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun UriHandler.openSafely(uri: String) {
    try {
        openUri(uri)
    } catch (_: IllegalArgumentException) {
    }
}

private fun List<PageBlock>.figureAlt(source: String): String? {
    forEach { block ->
        val alt = when (block) {
            is PageBlock.Figure -> if (block.source == source) block.alt else null
            is PageBlock.Quote -> block.blocks.figureAlt(source)
            is PageBlock.ListBlock -> block.items.firstNotNullOfOrNull { it.figureAlt(source) }
            else -> null
        }
        if (alt != null) return alt
    }
    return null
}
