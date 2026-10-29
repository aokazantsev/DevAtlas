package ru.aokazantsev.devatlas.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasCheckOption
import ru.aokazantsev.devatlas.designsystem.component.AtlasCheckboxDialog
import ru.aokazantsev.devatlas.designsystem.component.AtlasDivider
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasIcon
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSwitchRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun ComponentCatalog(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        AtlasTheme(darkTheme = false) {
            CatalogSection(themeName = "Светлая тема")
        }
        AtlasTheme(darkTheme = true) {
            CatalogSection(themeName = "Тёмная тема")
        }
    }
}

@Composable
private fun CatalogSection(themeName: String) {
    val spacing = AtlasTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AtlasTheme.colors.background)
            .padding(vertical = spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        AtlasText(text = themeName, style = AtlasTheme.typography.title, modifier = Modifier.padding(horizontal = spacing.l))
        CatalogGroup("Шапка") {
            AtlasTopBar(title = "Содержание", onBack = {}, windowInsets = WindowInsets(0, 0, 0, 0))
        }
        CatalogGroup("Цвета") { ColorSwatches() }
        CatalogGroup("Типографика") { TypographySamples() }
        CatalogGroup("Иконки") { IconSamples() }
        CatalogGroup("Строки списка") { ListRowSamples() }
        CatalogGroup("Кнопки") { ButtonSamples() }
        CatalogGroup("Переключатели") { ToggleSamples() }
        CatalogGroup("Шторка и диалог") { OverlaySamples() }
        CatalogGroup("Загрузка и ошибка") { StateSamples() }
    }
}

@Composable
private fun CatalogGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
        AtlasText(
            text = title,
            style = AtlasTheme.typography.label,
            color = AtlasTheme.colors.onSurfaceMuted,
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        )
        Column(modifier = Modifier.fillMaxWidth().background(AtlasTheme.colors.surface)) {
            content()
        }
    }
}

@Composable
private fun ColorSwatches() {
    val colors = AtlasTheme.colors
    val swatches = listOf(
        "background" to colors.background,
        "surface" to colors.surface,
        "surfaceRaised" to colors.surfaceRaised,
        "onSurface" to colors.onSurface,
        "onSurfaceMuted" to colors.onSurfaceMuted,
        "accent" to colors.accent,
        "onAccent" to colors.onAccent,
        "divider" to colors.divider,
        "success" to colors.success,
        "error" to colors.error,
    )
    FlowRow(
        modifier = Modifier.padding(AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        swatches.forEach { (name, color) -> Swatch(name, color) }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(AtlasTheme.sizes.iconLarge)
                .background(color, AtlasTheme.shapes.small)
                .border(AtlasTheme.sizes.divider, AtlasTheme.colors.divider, AtlasTheme.shapes.small),
        )
        AtlasText(text = name, style = AtlasTheme.typography.caption, color = AtlasTheme.colors.onSurfaceMuted)
    }
}

@Composable
private fun TypographySamples() {
    val typography = AtlasTheme.typography
    Column(
        modifier = Modifier.padding(AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.xs),
    ) {
        AtlasText(text = "Заголовок экрана", style = typography.title)
        AtlasText(text = "Заголовок раздела", style = typography.heading)
        AtlasText(text = "Основной текст страницы темы.")
        AtlasText(text = "Подпись кнопки", style = typography.label)
        AtlasText(text = "Мелкая подпись", style = typography.caption, color = AtlasTheme.colors.onSurfaceMuted)
        AtlasText(text = "val code = \"моноширинный\"", style = typography.code)
    }
}

@Composable
private fun IconSamples() {
    val icons = listOf(
        AtlasIcons.Check, AtlasIcons.CheckCircle, AtlasIcons.ChevronRight, AtlasIcons.ArrowBack,
        AtlasIcons.Settings, AtlasIcons.Info, AtlasIcons.Close, AtlasIcons.Error, AtlasIcons.Refresh,
        AtlasIcons.LightMode, AtlasIcons.DarkMode, AtlasIcons.Contrast, AtlasIcons.CheckBox,
        AtlasIcons.CheckBoxOutlineBlank,
    )
    FlowRow(
        modifier = Modifier.padding(AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        icons.forEach { AtlasIcon(icon = it, contentDescription = it.name) }
        AtlasIcon(icon = AtlasIcons.CheckCircle, contentDescription = null, tint = AtlasTheme.colors.accent)
    }
}

@Composable
private fun ListRowSamples() {
    AtlasListRow(title = "Страница", checked = false, onClick = {})
    AtlasDivider()
    AtlasListRow(title = "Просмотренная страница", checked = true, onClick = {})
    AtlasDivider()
    AtlasListRow(title = "Раздел", counter = "3 из 7", showChevron = true, onClick = {})
    AtlasDivider()
    AtlasListRow(title = "Пройденный раздел", counter = "7 из 7", checked = true, showChevron = true, onClick = {})
    AtlasDivider()
    AtlasListRow(title = "С иконкой и подписью", subtitle = "Подпись строки", leadingIcon = AtlasIcons.Settings, onClick = {})
}

@Composable
private fun ButtonSamples() {
    Column(
        modifier = Modifier.padding(AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasPrimaryButton(text = "Дальше", onClick = {}, modifier = Modifier.fillMaxWidth())
        AtlasPrimaryButton(text = "Недоступно", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
            AtlasSecondaryButton(text = "Отмена", onClick = {})
            AtlasTextButton(text = "Подробнее", onClick = {})
        }
    }
}

@Composable
private fun ToggleSamples() {
    var checked by remember { mutableStateOf(true) }
    AtlasSwitchRow(title = "Просмотрено", checked = checked, onCheckedChange = { checked = it })
    AtlasSwitchRow(title = "Выключено", subtitle = "Подпись переключателя", checked = false, onCheckedChange = {})
}

@Composable
private fun OverlaySamples() {
    var sheetShown by remember { mutableStateOf(false) }
    var dialogShown by remember { mutableStateOf(false) }
    var options by remember {
        mutableStateOf(
            listOf(
                AtlasCheckOption(key = "delete", title = "Удалить тему", checked = false),
                AtlasCheckOption(key = "reset", title = "Сбросить прогресс", checked = false),
            ),
        )
    }
    Row(
        modifier = Modifier.padding(AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasSecondaryButton(text = "Шторка", onClick = { sheetShown = true })
        AtlasSecondaryButton(text = "Диалог", onClick = { dialogShown = true })
    }
    if (sheetShown) {
        AtlasBottomSheet(title = "Что будет удалено", onDismiss = { sheetShown = false }) {
            AtlasText(text = "Тема и её файлы. Прогресс останется.", color = AtlasTheme.colors.onSurfaceMuted)
            AtlasPrimaryButton(text = "Подтвердить", onClick = { sheetShown = false }, modifier = Modifier.fillMaxWidth())
            AtlasSecondaryButton(text = "Отмена", onClick = { sheetShown = false }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (dialogShown) {
        AtlasCheckboxDialog(
            title = "Управление темой",
            options = options,
            confirmText = "Применить",
            onCheckedChange = { key, value ->
                options = options.map { if (it.key == key) it.copy(checked = value) else it }
            },
            onConfirm = { dialogShown = false },
            onDismiss = { dialogShown = false },
        )
    }
}

@Composable
private fun StateSamples() {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        AtlasLoading(text = "Загружаю каталог")
    }
    AtlasDivider()
    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        AtlasError(message = "Не удалось загрузить каталог", actionText = "Повторить")
    }
}

@Preview
@Composable
private fun ComponentCatalogLightPreview() {
    AtlasTheme(darkTheme = false) {
        CatalogSection(themeName = "Светлая тема")
    }
}

@Preview
@Composable
private fun ComponentCatalogDarkPreview() {
    AtlasTheme(darkTheme = true) {
        CatalogSection(themeName = "Тёмная тема")
    }
}
