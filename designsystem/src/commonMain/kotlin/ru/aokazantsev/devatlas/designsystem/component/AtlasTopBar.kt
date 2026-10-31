package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleOverflow: TextOverflow = TextOverflow.Ellipsis,
    subtitleOverflow: TextOverflow = TextOverflow.Ellipsis,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
) {
    TopAppBar(
        title = {
            Column {
                AtlasText(text = title, maxLines = 1, overflow = titleOverflow)
                if (subtitle != null) {
                    AtlasText(
                        text = subtitle,
                        style = AtlasTheme.typography.bodySmall,
                        color = AtlasTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = subtitleOverflow,
                    )
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                AtlasIconButton(icon = AtlasIcons.ArrowBack, contentDescription = "Назад", onClick = onBack)
            }
        },
        actions = actions,
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AtlasTheme.colors.surfaceContainer,
            scrolledContainerColor = AtlasTheme.colors.surfaceContainer,
        ),
    )
}
