package ru.aokazantsev.devatlas.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasChoiceGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasChoiceOption
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSwitchRow
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
    onOpenThemes: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenComponentCatalog: () -> Unit,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    AtlasScaffold(topBar = { AtlasTopBar(title = "Настройки", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        ) {
            SectionTitle("Оформление")
            AtlasChoiceGroup(
                options = ThemeModeOptions,
                selected = appearance?.themeMode,
                onSelect = viewModel::selectThemeMode,
            )
            if (AtlasTheme.wallpaperColorsAvailable) {
                AtlasListGroup {
                    AtlasSwitchRow(
                        title = "Цвета из обоев",
                        subtitle = "Цвета приложения подбираются под обои телефона",
                        checked = appearance?.wallpaperColors == true,
                        onCheckedChange = viewModel::setWallpaperColors,
                    )
                }
            }
            ThemesSection(onOpenThemes)
            SectionTitle("Приложение")
            val rows = if (showComponentCatalog) 2 else 1
            AtlasListGroup {
                AtlasListRow(
                    title = "О приложении",
                    leadingIcon = AtlasIcons.Info,
                    showChevron = true,
                    index = 0,
                    count = rows,
                    onClick = onOpenAbout,
                )
                if (showComponentCatalog) {
                    AtlasListRow(
                        title = "Каталог компонентов",
                        subtitle = "Только в отладочной сборке",
                        leadingIcon = AtlasIcons.Palette,
                        showChevron = true,
                        index = 1,
                        count = rows,
                        onClick = onOpenComponentCatalog,
                    )
                }
            }
        }
    }
}

private val ThemeModeOptions = listOf(
    AtlasChoiceOption(ThemeMode.LIGHT, "Светлая", AtlasIcons.LightMode),
    AtlasChoiceOption(ThemeMode.DARK, "Тёмная", AtlasIcons.DarkMode),
    AtlasChoiceOption(ThemeMode.SYSTEM, "Система", AtlasIcons.Contrast),
)

@Composable
private fun SectionTitle(text: String) {
    AtlasText(
        text = text,
        style = AtlasTheme.typography.labelLarge,
        color = AtlasTheme.colors.primary,
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l, vertical = AtlasTheme.spacing.s),
    )
}

@Composable
private fun ThemesSection(onOpenThemes: () -> Unit) {
    SectionTitle("Темы")
    AtlasListGroup {
        AtlasListRow(
            title = "Список тем",
            subtitle = "Применить, обновить, добавить или удалить",
            leadingIcon = AtlasIcons.Folder,
            showChevron = true,
            onClick = onOpenThemes,
        )
    }
}
