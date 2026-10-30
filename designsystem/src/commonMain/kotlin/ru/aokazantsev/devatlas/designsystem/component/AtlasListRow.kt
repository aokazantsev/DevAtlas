package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasListRow(
    title: String,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    counter: String? = null,
    checked: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shapes = atlasListItemShapes(index = index, count = count)
    val leading: (@Composable () -> Unit)? = leadingIcon?.let { { AtlasIcon(icon = it, contentDescription = null) } }
    val supporting: (@Composable () -> Unit)? = subtitle?.let { { AtlasText(text = it) } }
    val trailing: (@Composable () -> Unit)? = if (counter != null || checked || showChevron) {
        { RowTrailing(counter = counter, checked = checked, showChevron = showChevron) }
    } else {
        null
    }
    if (onClick != null) {
        SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            colors = atlasListItemColors(),
            modifier = modifier,
            leadingContent = leading,
            supportingContent = supporting,
            trailingContent = trailing,
        ) {
            AtlasText(text = title)
        }
    } else {
        ListItem(
            headlineContent = { AtlasText(text = title) },
            modifier = modifier.clip(shapes.shape),
            leadingContent = leading,
            supportingContent = supporting,
            trailingContent = trailing,
            colors = atlasListItemColors(),
        )
    }
}

@Composable
private fun RowTrailing(counter: String?, checked: Boolean, showChevron: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (counter != null) {
            AtlasText(text = counter, style = AtlasTheme.typography.labelLarge)
        }
        if (checked) {
            AtlasIcon(icon = AtlasIcons.CheckCircle, contentDescription = "Готово", tint = AtlasTheme.colors.primary)
        }
        if (showChevron) {
            AtlasIcon(icon = AtlasIcons.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun atlasListItemColors() = ListItemDefaults.segmentedColors(
    containerColor = AtlasTheme.colors.surface,
    selectedContainerColor = AtlasTheme.colors.surface,
    selectedContentColor = AtlasTheme.colors.onSurface,
    selectedSupportingContentColor = AtlasTheme.colors.onSurfaceVariant,
    selectedLeadingContentColor = AtlasTheme.colors.onSurfaceVariant,
    selectedTrailingContentColor = AtlasTheme.colors.onSurfaceVariant,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun atlasListItemShapes(index: Int, count: Int): ListItemShapes {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    return shapes.copy(selectedShape = shapes.shape)
}
