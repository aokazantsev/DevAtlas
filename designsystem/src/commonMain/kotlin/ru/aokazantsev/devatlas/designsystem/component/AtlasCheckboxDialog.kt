package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasCheckboxDialog(
    title: String,
    options: List<AtlasCheckOption>,
    confirmText: String,
    onCheckedChange: (key: String, checked: Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Отмена",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { AtlasText(text = title, style = AtlasTheme.typography.heading) },
        text = {
            Column {
                options.forEach { option ->
                    AtlasCheckboxRow(
                        title = option.title,
                        checked = option.checked,
                        onCheckedChange = { onCheckedChange(option.key, it) },
                    )
                }
            }
        },
        confirmButton = {
            AtlasTextButton(text = confirmText, onClick = onConfirm, enabled = options.any { it.checked })
        },
        dismissButton = {
            AtlasTextButton(text = dismissText, onClick = onDismiss)
        },
        shape = AtlasTheme.shapes.large,
        containerColor = AtlasTheme.colors.surface,
        titleContentColor = AtlasTheme.colors.onSurface,
        textContentColor = AtlasTheme.colors.onSurface,
    )
}
