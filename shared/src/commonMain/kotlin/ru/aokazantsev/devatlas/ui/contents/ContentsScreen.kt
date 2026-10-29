package ru.aokazantsev.devatlas.ui.contents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun ContentsScreen(onOpenPage: (String) -> Unit, onOpenSettings: () -> Unit) {
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = "Содержание",
                actions = {
                    AtlasIconButton(icon = AtlasIcons.Settings, contentDescription = "Настройки", onClick = onOpenSettings)
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AtlasText(
                text = "Темы появятся в следующих версиях.",
                color = AtlasTheme.colors.onSurfaceMuted,
                modifier = Modifier.padding(AtlasTheme.spacing.l),
            )
            AtlasListRow(title = "Пример страницы", showChevron = true, onClick = { onOpenPage(SAMPLE_PAGE_ID) })
        }
    }
}

private const val SAMPLE_PAGE_ID = "sample"
