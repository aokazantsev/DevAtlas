package ru.aokazantsev.devatlas.ui.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasProgressBar
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.text.russianPlural
import ru.aokazantsev.devatlas.ui.bookmarks.AddBookmarkButton
import ru.aokazantsev.devatlas.ui.bookmarks.NewBookmarkSheet
import ru.aokazantsev.devatlas.ui.scroll.RecordPosition
import ru.aokazantsev.devatlas.ui.scroll.RestoreAnchor
import ru.aokazantsev.devatlas.ui.scroll.ScrollPosition
import ru.aokazantsev.devatlas.ui.scroll.rememberScrollPosition

@Composable
fun ContentsScreen(
    viewModel: ContentsViewModel,
    onOpenSection: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenSettings: (() -> Unit)?,
    onOpenBookmarks: (() -> Unit)?,
    onAddTheme: () -> Unit,
    onBack: (() -> Unit)?,
    anchor: Int,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? ContentsState.Ready
    val title = ready?.title ?: "Содержание"
    val position = rememberScrollPosition(anchor)
    var bookmarkAnchor by rememberSaveable { mutableStateOf<Int?>(null) }
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (onOpenBookmarks != null && ready != null) {
                        AtlasIconButton(icon = AtlasIcons.Bookmarks, contentDescription = "Закладки", onClick = onOpenBookmarks)
                    }
                    if (ready != null) {
                        AddBookmarkButton(bookmarked = ready.bookmarked, onClick = { bookmarkAnchor = position.current() ?: 0 })
                    }
                    if (onOpenSettings != null) {
                        AtlasIconButton(icon = AtlasIcons.Settings, contentDescription = "Настройки", onClick = onOpenSettings)
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val shown = state) {
            ContentsState.Loading -> AtlasLoading(modifier = modifier)
            ContentsState.NoTheme -> AtlasError(
                message = "Тема не выбрана",
                actionText = "Добавить тему",
                onAction = onAddTheme,
                modifier = modifier,
            )
            ContentsState.Unreadable -> AtlasError(
                message = "Содержание темы не читается. Перезапустите приложение — оно предложит скачать тему заново.",
                modifier = modifier,
            )
            ContentsState.SectionMissing -> AtlasError(
                message = "Этого раздела больше нет в теме",
                actionText = onBack?.let { "Назад" },
                onAction = { onBack?.invoke() },
                modifier = modifier,
            )
            is ContentsState.Ready -> ContentsList(
                state = shown,
                position = position,
                onRecordPosition = viewModel::recordPosition,
                onOpenSection = onOpenSection,
                onOpenPage = onOpenPage,
                modifier = modifier,
            )
        }
    }
    NewBookmarkSheet(anchor = bookmarkAnchor, defaultName = ready?.title, onSave = viewModel::addBookmark) { bookmarkAnchor = null }
}

@Composable
private fun ContentsList(
    state: ContentsState.Ready,
    position: ScrollPosition,
    onRecordPosition: (Int) -> Unit,
    onOpenSection: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier,
) {
    val anchors = position.anchors
    RestoreAnchor(position)
    RecordPosition(position, onRecordPosition)
    Column(
        modifier = modifier
            .verticalScroll(position.scrollState)
            .then(anchors.contentModifier)
            .padding(bottom = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
    ) {
        state.description?.let {
            AtlasText(
                text = it,
                color = AtlasTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            )
        }
        state.progress?.let { OverallProgress(it) }
        AtlasListGroup {
            state.rows.forEachIndexed { index, row ->
                when (row) {
                    is ContentsRow.Page -> AtlasListRow(
                        title = row.title,
                        modifier = anchors.item(index),
                        index = index,
                        count = state.rows.size,
                        leadingIcon = AtlasIcons.Article,
                        checked = row.viewed,
                        onClick = { onOpenPage(row.id) },
                    )
                    is ContentsRow.Section -> AtlasListRow(
                        title = row.title,
                        modifier = anchors.item(index),
                        index = index,
                        count = state.rows.size,
                        leadingIcon = AtlasIcons.Folder,
                        subtitle = "${row.progress.viewed} из ${row.progress.total}",
                        checked = row.progress.total > 0 && row.progress.viewed == row.progress.total,
                        showChevron = true,
                        onClick = { onOpenSection(row.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallProgress(progress: ContentsProgress) {
    Column(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        val pages = russianPlural(progress.total, one = "страницы", few = "страниц", many = "страниц")
        AtlasText(
            text = "Просмотрено ${progress.viewed} из ${progress.total} $pages",
            style = AtlasTheme.typography.labelLarge,
        )
        AtlasProgressBar(progress = progress.fraction)
    }
}
