package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        title = { AtlasText(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                options.forEachIndexed { index, option ->
                    AtlasCheckboxRow(
                        title = option.title,
                        checked = option.checked,
                        onCheckedChange = { onCheckedChange(option.key, it) },
                        index = index,
                        count = options.size,
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
    )
}
