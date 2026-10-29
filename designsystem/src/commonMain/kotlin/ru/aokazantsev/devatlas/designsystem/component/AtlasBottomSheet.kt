package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = AtlasTheme.spacing
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AtlasTheme.shapes.sheet,
        containerColor = AtlasTheme.colors.surface,
        contentColor = AtlasTheme.colors.onSurface,
        scrimColor = AtlasTheme.colors.scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.l, vertical = spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            if (title != null) {
                AtlasText(text = title, style = AtlasTheme.typography.heading)
            }
            content()
        }
    }
}
