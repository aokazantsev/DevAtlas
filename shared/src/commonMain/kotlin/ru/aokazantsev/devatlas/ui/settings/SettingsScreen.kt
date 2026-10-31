package ru.aokazantsev.devatlas.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasChoiceGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasChoiceOption
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSwitchRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.ui.onboarding.ConsentDetails

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    showComponentCatalog: Boolean,
    onBack: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenComponentCatalog: () -> Unit,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val continueFromLastPoint by viewModel.continueFromLastPoint.collectAsStateWithLifecycle()
    val consent by viewModel.consent.collectAsStateWithLifecycle()
    val autoUpdate by viewModel.autoUpdate.collectAsStateWithLifecycle()
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
            ReadingSection(continueFromLastPoint, viewModel::setContinueFromLastPoint)
            ThemesSection(onOpenThemes)
            FilesSection(onOpenDownloads)
            DataSection(consent, viewModel::setCrashReports, viewModel::setUsageStatistics)
            AppSection(
                autoUpdate = autoUpdate,
                onAutoUpdate = viewModel::setAutoUpdate,
                showComponentCatalog = showComponentCatalog,
                onOpenAbout = onOpenAbout,
                onOpenComponentCatalog = onOpenComponentCatalog,
            )
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
private fun ReadingSection(continueFromLastPoint: Boolean?, onChange: (Boolean) -> Unit) {
    SectionTitle("Чтение")
    AtlasListGroup {
        AtlasSwitchRow(
            title = "Продолжать с последней точки",
            subtitle = "При запуске открывать экран и место, где вы остановились",
            checked = continueFromLastPoint == true,
            onCheckedChange = onChange,
            enabled = continueFromLastPoint != null,
        )
    }
}

@Composable
private fun AppSection(
    autoUpdate: Boolean?,
    onAutoUpdate: (Boolean) -> Unit,
    showComponentCatalog: Boolean,
    onOpenAbout: () -> Unit,
    onOpenComponentCatalog: () -> Unit,
) {
    SectionTitle("Приложение")
    val rows = if (showComponentCatalog) APP_ROWS_WITH_CATALOG else APP_ROWS
    AtlasListGroup {
        AtlasSwitchRow(
            title = "Проверять обновления",
            subtitle = "Раз в сутки смотреть на GitHub, вышла ли новая версия",
            checked = autoUpdate == true,
            onCheckedChange = onAutoUpdate,
            enabled = autoUpdate != null,
            index = 0,
            count = rows,
        )
        AtlasListRow(
            title = "О приложении",
            leadingIcon = AtlasIcons.Info,
            showChevron = true,
            index = 1,
            count = rows,
            onClick = onOpenAbout,
        )
        if (showComponentCatalog) {
            AtlasListRow(
                title = "Каталог компонентов",
                subtitle = "Только в отладочной сборке",
                leadingIcon = AtlasIcons.Palette,
                showChevron = true,
                index = 2,
                count = rows,
                onClick = onOpenComponentCatalog,
            )
        }
    }
}

@Composable
private fun DataSection(consent: Consent?, onCrashReports: (Boolean) -> Unit, onUsageStatistics: (Boolean) -> Unit) {
    var details by remember { mutableStateOf<ConsentDetails?>(null) }
    SectionTitle("Данные")
    AtlasListGroup {
        AtlasSwitchRow(
            title = "Отправлять отчёты о сбоях",
            subtitle = "Отчёт уходит один раз при следующем запуске после сбоя",
            checked = consent?.crashReports == true,
            onCheckedChange = onCrashReports,
            enabled = consent != null,
            index = 0,
            count = 2,
        )
        AtlasSwitchRow(
            title = "Отправлять статистику",
            subtitle = "Сколько раз за сутки использовались функции, без названий и адресов тем",
            checked = consent?.usageStatistics == true,
            onCheckedChange = onUsageStatistics,
            enabled = consent != null,
            index = 1,
            count = 2,
        )
    }
    Column(modifier = Modifier.padding(horizontal = AtlasTheme.spacing.s)) {
        AtlasTextButton(text = ConsentDetails.CrashReport.title, onClick = { details = ConsentDetails.CrashReport })
        AtlasTextButton(text = ConsentDetails.Statistics.title, onClick = { details = ConsentDetails.Statistics })
    }
    val shown = details ?: return
    AtlasBottomSheet(title = shown.title, onDismiss = { details = null }) {
        AtlasText(text = shown.text, color = AtlasTheme.colors.onSurfaceVariant)
        AtlasTextButton(text = "Закрыть", onClick = { details = null })
    }
}

@Composable
private fun FilesSection(onOpenDownloads: () -> Unit) {
    SectionTitle("Файлы")
    AtlasListGroup {
        AtlasListRow(
            title = "Скачанные файлы",
            subtitle = "PDF со страниц тем и сайтов в папке Загрузки/DevAtlas",
            leadingIcon = AtlasIcons.Download,
            showChevron = true,
            onClick = onOpenDownloads,
        )
    }
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

private const val APP_ROWS = 2
private const val APP_ROWS_WITH_CATALOG = 3
