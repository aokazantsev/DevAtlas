package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    counter: String? = null,
    checked: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val spacing = AtlasTheme.spacing
    val colors = AtlasTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AtlasTheme.sizes.listRowMinHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.l, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        if (leadingIcon != null) {
            AtlasIcon(icon = leadingIcon, contentDescription = null, tint = colors.onSurfaceMuted)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            AtlasText(text = title)
            if (subtitle != null) {
                AtlasText(text = subtitle, style = AtlasTheme.typography.caption, color = colors.onSurfaceMuted)
            }
        }
        if (counter != null) {
            AtlasText(text = counter, style = AtlasTheme.typography.label, color = colors.onSurfaceMuted)
        }
        if (checked) {
            AtlasIcon(icon = AtlasIcons.CheckCircle, contentDescription = "Готово", tint = colors.success)
        }
        if (showChevron) {
            AtlasIcon(icon = AtlasIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceMuted)
        }
    }
}

@Composable
fun AtlasDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = AtlasTheme.sizes.divider, color = AtlasTheme.colors.divider)
}
