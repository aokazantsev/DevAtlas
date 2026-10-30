package ru.aokazantsev.devatlas.ui.bookmarks

import androidx.compose.runtime.Composable

@Composable
fun NewBookmarkSheet(
    anchor: Int?,
    defaultName: String?,
    onSave: (name: String, anchor: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (anchor == null || defaultName == null) return
    BookmarkNameSheet(
        title = "Новая закладка",
        initialName = defaultName,
        onSave = { name ->
            onSave(name, anchor)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
