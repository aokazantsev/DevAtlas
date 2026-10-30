package ru.aokazantsev.devatlas.ui.bookmarks

import androidx.compose.runtime.Composable
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons

@Composable
fun AddBookmarkButton(bookmarked: Boolean, onClick: () -> Unit) {
    AtlasIconButton(
        icon = if (bookmarked) AtlasIcons.BookmarkAdded else AtlasIcons.BookmarkAdd,
        contentDescription = "Добавить закладку",
        onClick = onClick,
        stateDescription = if (bookmarked) "На этом экране уже есть закладка" else null,
    )
}
