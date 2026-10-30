package ru.aokazantsev.devatlas.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasReorderableGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasRowAction
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget

@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onBack: () -> Unit,
    onOpen: (List<Route>) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editedId by rememberSaveable { mutableStateOf<Long?>(null) }
    AtlasScaffold(topBar = { AtlasTopBar(title = "Закладки", onBack = onBack) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val shown = state) {
            BookmarksState.Loading -> AtlasLoading(modifier = modifier)
            BookmarksState.NoTheme -> AtlasError(message = "Закладки недоступны: тема не открывается", modifier = modifier)
            is BookmarksState.Ready -> BookmarkList(
                items = shown.items,
                onOpen = onOpen,
                onEdit = { editedId = it },
                onMove = viewModel::move,
                modifier = modifier,
            )
        }
    }
    val edited = (state as? BookmarksState.Ready)?.items?.firstOrNull { it.id == editedId } ?: return
    BookmarkNameSheet(
        title = "Закладка",
        initialName = edited.title,
        onSave = { name ->
            viewModel.rename(edited.id, name)
            editedId = null
        },
        onDismiss = { editedId = null },
        onDelete = {
            viewModel.delete(edited.id)
            editedId = null
        },
    )
}

@Composable
private fun BookmarkList(
    items: List<BookmarkItem>,
    onOpen: (List<Route>) -> Unit,
    onEdit: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
    ) {
        val hint = if (items.isEmpty()) {
            "Закладок пока нет. Чтобы добавить, нажмите значок закладки в шапке содержания, раздела или страницы."
        } else {
            "Нажмите закладку, чтобы открыть. Удерживайте и перетащите, чтобы поменять порядок."
        }
        AtlasText(
            text = hint,
            color = AtlasTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        )
        AtlasReorderableGroup(items = items, itemKey = { it.id }, onMove = onMove) { item, index, count, rowModifier ->
            AtlasListRow(
                title = item.title,
                modifier = rowModifier,
                subtitle = item.place,
                leadingIcon = when (item.target) {
                    ScreenTarget.Root -> AtlasIcons.Bookmarks
                    is ScreenTarget.Section -> AtlasIcons.Folder
                    is ScreenTarget.Page -> AtlasIcons.Article
                },
                index = index,
                count = count,
                trailingAction = AtlasRowAction(AtlasIcons.Edit, "Изменить закладку «${item.title}»", onClick = { onEdit(item.id) }),
                onClick = { onOpen(item.stack) },
            )
        }
    }
}
