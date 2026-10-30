package ru.aokazantsev.devatlas.ui.themes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.ui.format.pagesText

@Composable
fun ThemesScreen(
    viewModel: ThemesViewModel,
    onBack: () -> Unit,
    onOpenTheme: (themeId: String) -> Unit,
    onAddTheme: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AtlasScaffold(topBar = { AtlasTopBar(title = "Темы", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
        ) {
            val themes = state?.themes.orEmpty()
            if (themes.isNotEmpty()) {
                AtlasListGroup {
                    themes.forEachIndexed { index, theme ->
                        val current = if (theme.id == state?.currentId) "Текущая · " else ""
                        AtlasListRow(
                            title = theme.title,
                            subtitle = current + pagesText(theme.pageCount),
                            showChevron = true,
                            index = index,
                            count = themes.size,
                            onClick = { onOpenTheme(theme.id) },
                        )
                    }
                }
            } else {
                AtlasText(
                    text = "Тем пока нет",
                    color = AtlasTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
                )
            }
            AtlasListGroup {
                AtlasListRow(title = "Добавить тему", leadingIcon = AtlasIcons.Add, onClick = onAddTheme)
            }
        }
    }
}
