package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = atlasListItemShapes(index = index, count = count),
        modifier = modifier.semantics { role = Role.Switch },
        enabled = enabled,
        colors = atlasListItemColors(),
        supportingContent = subtitle?.let { { AtlasText(text = it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = if (checked) {
                    { AtlasIcon(icon = AtlasIcons.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
            )
        },
    ) {
        AtlasText(text = title)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = atlasListItemShapes(index = index, count = count),
        modifier = modifier,
        enabled = enabled,
        colors = atlasListItemColors(),
        supportingContent = subtitle?.let { { AtlasText(text = it) } },
        leadingContent = { Checkbox(checked = checked, onCheckedChange = null, enabled = enabled) },
    ) {
        AtlasText(text = title)
    }
}
