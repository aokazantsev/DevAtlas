package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = AtlasTheme.colors
    ToggleRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        enabled = enabled,
        role = Role.Switch,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.onSurfaceMuted,
                uncheckedTrackColor = colors.surfaceRaised,
                uncheckedBorderColor = colors.divider,
            ),
        )
    }
}

@Composable
fun AtlasCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = AtlasTheme.colors
    ToggleRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        enabled = enabled,
        role = Role.Checkbox,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    ) {
        AtlasIcon(
            icon = if (checked) AtlasIcons.CheckBox else AtlasIcons.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked && enabled) colors.accent else colors.onSurfaceMuted,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    role: Role,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    control: @Composable () -> Unit,
) {
    val spacing = AtlasTheme.spacing
    val colors = AtlasTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AtlasTheme.sizes.listRowMinHeight)
            .toggleable(value = checked, enabled = enabled, role = role, onValueChange = onCheckedChange)
            .padding(horizontal = spacing.l, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            AtlasText(text = title, color = if (enabled) colors.onSurface else colors.onSurfaceMuted)
            if (subtitle != null) {
                AtlasText(text = subtitle, style = AtlasTheme.typography.caption, color = colors.onSurfaceMuted)
            }
        }
        control()
    }
}
