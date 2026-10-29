package ru.aokazantsev.devatlas.ui.page

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun PageScreen(pageId: String, onBack: () -> Unit) {
    AtlasScaffold(topBar = { AtlasTopBar(title = "Страница", onBack = onBack) }) { padding ->
        AtlasText(
            text = "Здесь будет страница «$pageId».",
            color = AtlasTheme.colors.onSurfaceMuted,
            modifier = Modifier.padding(padding).padding(AtlasTheme.spacing.l),
        )
    }
}
