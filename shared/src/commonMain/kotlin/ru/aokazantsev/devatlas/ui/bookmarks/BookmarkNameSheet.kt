package ru.aokazantsev.devatlas.ui.bookmarks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextField

@Composable
fun BookmarkNameSheet(
    title: String,
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName.take(BookmarkNames.MAX_LENGTH)) }
    AtlasBottomSheet(title = title, onDismiss = onDismiss) {
        AtlasTextField(
            value = name,
            onValueChange = { name = it.take(BookmarkNames.MAX_LENGTH) },
            label = "Название закладки",
            keyboardType = KeyboardType.Text,
        )
        AtlasPrimaryButton(
            text = "Сохранить",
            onClick = { onSave(name.trim()) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (onDelete != null) {
            AtlasSecondaryButton(text = "Удалить закладку", onClick = onDelete, modifier = Modifier.fillMaxWidth())
        }
        AtlasSecondaryButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}
