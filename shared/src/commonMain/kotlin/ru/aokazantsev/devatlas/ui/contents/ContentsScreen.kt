package ru.aokazantsev.devatlas.ui.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ru.aokazantsev.devatlas.ui.scroll.RestoreAnchor
import ru.aokazantsev.devatlas.ui.scroll.ScrollAnchors

@Composable
fun ContentsScreen(
    viewModel: ContentsViewModel,
    onOpenSection: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenSettings: (() -> Unit)?,
    onAddTheme: () -> Unit,
    onBack: (() -> Unit)?,
    anchor: Int = 0,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = (state as? ContentsState.Ready)?.title ?: "Содержание"
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = title,
                onBack = onBack,
                actions = {
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
                anchor = anchor,
                onOpenSection = onOpenSection,
                onOpenPage = onOpenPage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ContentsList(
    state: ContentsState.Ready,
    anchor: Int,
    onOpenSection: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    modifier: Modifier,
) {
    val scrollState = rememberScrollState()
    val anchors = remember { ScrollAnchors() }
    RestoreAnchor(anchors, scrollState, anchor)
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
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
