package ru.aokazantsev.devatlas.designsystem.component

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
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
) {
    TopAppBar(
        title = { AtlasText(text = title, style = AtlasTheme.typography.heading, maxLines = 1) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                AtlasIconButton(icon = AtlasIcons.ArrowBack, contentDescription = "Назад", onClick = onBack)
            }
        },
        actions = actions,
        expandedHeight = AtlasTheme.sizes.topBarHeight,
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AtlasTheme.colors.background,
            titleContentColor = AtlasTheme.colors.onSurface,
            navigationIconContentColor = AtlasTheme.colors.onSurface,
            actionIconContentColor = AtlasTheme.colors.onSurface,
        ),
    )
}
