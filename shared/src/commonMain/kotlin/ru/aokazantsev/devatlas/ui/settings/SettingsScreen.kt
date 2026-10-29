package ru.aokazantsev.devatlas.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasDivider
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    showComponentCatalog: Boolean,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenComponentCatalog: () -> Unit,
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    AtlasScaffold(topBar = { AtlasTopBar(title = "Настройки", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Оформление")
            ThemeModeRow(ThemeMode.LIGHT, "Светлая", themeMode, viewModel::selectThemeMode)
            ThemeModeRow(ThemeMode.DARK, "Тёмная", themeMode, viewModel::selectThemeMode)
            ThemeModeRow(ThemeMode.SYSTEM, "Как в системе", themeMode, viewModel::selectThemeMode)
            AtlasDivider()
            AtlasListRow(title = "О приложении", leadingIcon = AtlasIcons.Info, showChevron = true, onClick = onOpenAbout)
            if (showComponentCatalog) {
                AtlasListRow(
                    title = "Каталог компонентов",
                    subtitle = "Только в отладочной сборке",
                    leadingIcon = AtlasIcons.Contrast,
                    showChevron = true,
                    onClick = onOpenComponentCatalog,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    AtlasText(
        text = text,
        style = AtlasTheme.typography.label,
        color = AtlasTheme.colors.onSurfaceMuted,
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l, vertical = AtlasTheme.spacing.s),
    )
}

@Composable
private fun ThemeModeRow(mode: ThemeMode, title: String, selected: ThemeMode?, onSelect: (ThemeMode) -> Unit) {
    AtlasListRow(
        title = title,
        leadingIcon = when (mode) {
            ThemeMode.LIGHT -> AtlasIcons.LightMode
            ThemeMode.DARK -> AtlasIcons.DarkMode
            ThemeMode.SYSTEM -> AtlasIcons.Contrast
        },
        checked = mode == selected,
        onClick = { onSelect(mode) },
    )
}
