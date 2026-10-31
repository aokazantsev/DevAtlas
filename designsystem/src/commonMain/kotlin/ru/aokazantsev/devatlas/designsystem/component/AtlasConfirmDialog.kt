package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AtlasConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Отмена",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { AtlasText(text = title) },
        text = { AtlasText(text = text, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { AtlasTextButton(text = confirmText, onClick = onConfirm) },
        dismissButton = { AtlasTextButton(text = dismissText, onClick = onDismiss) },
    )
}
