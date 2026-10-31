package ru.aokazantsev.devatlas.ui.downloads

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasRowAction
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.text.russianPlural

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var actionsFor by remember { mutableStateOf<DownloadRow?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    val rows = (state as? DownloadsState.Ready)?.rows.orEmpty()
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = "Скачанные файлы",
                onBack = onBack,
                actions = {
                    if (rows.isNotEmpty()) {
                        AtlasIconButton(
                            icon = AtlasIcons.DeleteSweep,
                            contentDescription = "Удалить все",
                            onClick = { confirmDeleteAll = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when {
            state == DownloadsState.Loading -> AtlasLoading(modifier = modifier)
            rows.isEmpty() -> Column(modifier = modifier.padding(AtlasTheme.spacing.xl)) {
                AtlasText(
                    text = "Здесь появятся PDF, скачанные со страниц тем и сайтов. Файлы лежат в папке Загрузки/DevAtlas.",
                    color = AtlasTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> DownloadList(rows, onOpen = viewModel::open, onActions = { actionsFor = it }, modifier = modifier)
        }
    }
    DownloadsOverlays(
        viewModel = viewModel,
        actionsFor = actionsFor,
        onActionsDone = { actionsFor = null },
        confirmDeleteAll = confirmDeleteAll,
        onDeleteAllDone = { confirmDeleteAll = false },
        count = rows.size,
    )
}

@Composable
private fun DownloadsOverlays(
    viewModel: DownloadsViewModel,
    actionsFor: DownloadRow?,
    onActionsDone: () -> Unit,
    confirmDeleteAll: Boolean,
    onDeleteAllDone: () -> Unit,
    count: Int,
) {
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    actionsFor?.let { row ->
        FileActionsSheet(
            row = row,
            onShare = {
                onActionsDone()
                viewModel.share(row)
            },
            onDelete = {
                onActionsDone()
                viewModel.delete(row)
            },
            onDismiss = onActionsDone,
        )
    }
    if (confirmDeleteAll) {
        DeleteAllSheet(
            count = count,
            onConfirm = {
                onDeleteAllDone()
                viewModel.deleteAll()
            },
            onDismiss = onDeleteAllDone,
        )
    }
    notice?.let { message ->
        AtlasBottomSheet(title = "Скачанные файлы", onDismiss = viewModel::dismissNotice) {
            AtlasText(text = message)
            AtlasPrimaryButton(text = "Понятно", onClick = viewModel::dismissNotice, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DownloadList(rows: List<DownloadRow>, onOpen: (DownloadRow) -> Unit, onActions: (DownloadRow) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = AtlasTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasListGroup {
            rows.forEachIndexed { index, row ->
                AtlasListRow(
                    title = row.file.name,
                    subtitle = row.details,
                    leadingIcon = AtlasIcons.PictureAsPdf,
                    index = index,
                    count = rows.size,
                    trailingAction = AtlasRowAction(
                        icon = AtlasIcons.MoreVert,
                        contentDescription = "Действия с файлом ${row.file.name}",
                        onClick = { onActions(row) },
                    ),
                    onClick = { onOpen(row) },
                )
            }
        }
    }
}

@Composable
private fun FileActionsSheet(row: DownloadRow, onShare: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AtlasBottomSheet(title = row.file.name, onDismiss = onDismiss) {
        AtlasText(text = row.details, color = AtlasTheme.colors.onSurfaceVariant)
        AtlasPrimaryButton(text = "Поделиться", onClick = onShare, modifier = Modifier.fillMaxWidth())
        AtlasSecondaryButton(text = "Удалить файл", onClick = onDelete, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DeleteAllSheet(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val files = "$count ${russianPlural(count, one = "файл", few = "файла", many = "файлов")}"
    AtlasBottomSheet(title = "Удалить все файлы?", onDismiss = onDismiss) {
        AtlasText(
            text = "$files удалятся из папки Загрузки/DevAtlas и из этого списка. Страницы тем останутся, PDF можно скачать снова.",
            color = AtlasTheme.colors.onSurfaceVariant,
        )
        AtlasPrimaryButton(text = "Удалить все", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        AtlasSecondaryButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}
