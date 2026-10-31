package ru.aokazantsev.devatlas.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.ui.bookmarks.AddBookmarkButton
import ru.aokazantsev.devatlas.ui.bookmarks.NewBookmarkSheet
import ru.aokazantsev.devatlas.ui.links.LinkActions
import ru.aokazantsev.devatlas.ui.page.content.PageBlocks
import ru.aokazantsev.devatlas.ui.page.content.PageLinks
import ru.aokazantsev.devatlas.ui.page.diagram.DiagramViewer
import ru.aokazantsev.devatlas.ui.page.diagram.SvgColorTokens
import ru.aokazantsev.devatlas.ui.page.diagram.rememberSvgPalette
import ru.aokazantsev.devatlas.ui.scroll.RecordPosition
import ru.aokazantsev.devatlas.ui.scroll.RestoreAnchor
import ru.aokazantsev.devatlas.ui.scroll.ScrollPosition
import ru.aokazantsev.devatlas.ui.scroll.rememberScrollPosition

@Composable
fun PageScreen(
    viewModel: PageViewModel,
    anchor: Int,
    links: LinkActions,
    onNext: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = when (val shown = state) {
        is PageState.Ready -> shown.title
        is PageState.Unreadable -> shown.title
        else -> null
    } ?: "Страница"
    val position = rememberScrollPosition(anchor)
    var bookmarkAnchor by rememberSaveable { mutableStateOf<Int?>(null) }
    val ready = state as? PageState.Ready
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (ready != null) {
                        AddBookmarkButton(bookmarked = ready.bookmarked, onClick = { bookmarkAnchor = position.current() ?: 0 })
                    }
                },
            )
        },
    ) { padding ->
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
                position = position,
                onRecordPosition = viewModel::recordPosition,
                onViewedChange = viewModel::setViewed,
                onCopyCode = viewModel::copyCode,
                onDiagramOpened = viewModel::openedDiagram,
                links = links,
                onNext = onNext,
                modifier = modifier,
            )
        }
    }
    NewBookmarkSheet(anchor = bookmarkAnchor, defaultName = ready?.title, onSave = viewModel::addBookmark) { bookmarkAnchor = null }
}

@Composable
private fun PageBody(
    state: PageState.Ready,
    position: ScrollPosition,
    onRecordPosition: (Int) -> Unit,
    onViewedChange: (Boolean) -> Unit,
    onCopyCode: (String) -> Boolean,
    onDiagramOpened: () -> Unit,
    links: LinkActions,
    onNext: (String) -> Unit,
    modifier: Modifier,
) {
    var menuLink by remember { mutableStateOf<PageInline.Link?>(null) }
    val pageLinks = remember(links, state.title) {
        PageLinks(
            onClick = { link -> links.follow(link, state.title) },
            onLongPress = { link -> if (link.externalUri() != null) menuLink = link },
        )
    }
    var openedDiagram by rememberSaveable { mutableStateOf<String?>(null) }
    RestoreAnchor(position)
    RecordPosition(position, onRecordPosition)
    Column(
        modifier = modifier
            .verticalScroll(position.scrollState)
            .then(position.anchors.contentModifier)
            .padding(vertical = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.xl),
    ) {
        PageBlocks(
            blocks = state.content.document.blocks,
            diagrams = state.content.diagrams,
            links = pageLinks,
            onOpenDiagram = {
                openedDiagram = it.source
                onDiagramOpened()
            },
            onCopyCode = onCopyCode,
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            anchors = position.anchors,
        )
        AtlasListGroup {
            AtlasSwitchRow(title = "Просмотрено", checked = state.viewed, onCheckedChange = onViewedChange)
        }
        state.next?.let { next -> NextPageButton(next, onNext) }
    }
    LinkMenuSheet(link = menuLink, pageTitle = state.title, actions = links) { menuLink = null }
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
