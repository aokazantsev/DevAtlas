package ru.aokazantsev.devatlas.designsystem.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasCheckOption
import ru.aokazantsev.devatlas.designsystem.component.AtlasCheckboxDialog
import ru.aokazantsev.devatlas.designsystem.component.AtlasChoiceGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasChoiceOption
import ru.aokazantsev.devatlas.designsystem.component.AtlasConfirmDialog
import ru.aokazantsev.devatlas.designsystem.component.AtlasDivider
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasIcon
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoadingIndicator
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasProgressBar
import ru.aokazantsev.devatlas.designsystem.component.AtlasReorderableGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasRowAction
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSwitchRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextField
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun ComponentCatalog(modifier: Modifier = Modifier) {
    var wallpaperColors by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l)) {
        AtlasListGroup {
            AtlasSwitchRow(
                title = "Цвета из обоев",
                subtitle = if (AtlasTheme.wallpaperColorsAvailable) "Android 12 и новее" else "Недоступно ниже Android 12",
                checked = wallpaperColors,
                enabled = AtlasTheme.wallpaperColorsAvailable,
                onCheckedChange = { wallpaperColors = it },
            )
        }
        AtlasTheme(darkTheme = false, wallpaperColors = wallpaperColors) {
            CatalogSection(themeName = "Светлая тема")
        }
        AtlasTheme(darkTheme = true, wallpaperColors = wallpaperColors) {
            CatalogSection(themeName = "Тёмная тема")
        }
    }
}

@Composable
private fun CatalogSection(themeName: String) {
    Surface(color = AtlasTheme.colors.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
        ) {
            AtlasText(
                text = themeName,
                style = AtlasTheme.typography.emphasized.headlineSmall,
                modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            )
            CatalogGroup("Шапка") {
                AtlasTopBar(title = "Содержание", onBack = {}, windowInsets = WindowInsets(0, 0, 0, 0))
            }
            CatalogGroup("Цвета") { ColorRoles() }
            CatalogGroup("Типографика") { TypographySamples() }
            CatalogGroup("Формы") { ShapeSamples() }
            CatalogGroup("Иконки") { IconSamples() }
            CatalogGroup("Строки списка") { ListRowSamples() }
            CatalogGroup("Перестановка удержанием") { ReorderSamples() }
            CatalogGroup("Кнопки") { ButtonSamples() }
            CatalogGroup("Группа выбора") { ChoiceSamples() }
            CatalogGroup("Переключатели") { ToggleSamples() }
            CatalogGroup("Поле ввода") { TextFieldSamples() }
            CatalogGroup("Прогресс") { ProgressSamples() }
            CatalogGroup("Разделитель текста") { DividerSample() }
            CatalogGroup("Шторка и диалог") { OverlaySamples() }
            CatalogGroup("Загрузка и ошибка") { StateSamples() }
        }
    }
}

@Composable
private fun CatalogLabel(text: String) {
    AtlasText(
        text = text,
        style = AtlasTheme.typography.labelLarge,
        color = AtlasTheme.colors.primary,
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
    )
}

@Composable
private fun CatalogGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s)) {
        CatalogLabel(title)
        content()
    }
}

@Composable
private fun ColorRoles() {
    val colors = AtlasTheme.colors
    val roles = listOf(
        "primary" to colors.primary,
        "onPrimary" to colors.onPrimary,
        "primaryContainer" to colors.primaryContainer,
        "secondary" to colors.secondary,
        "secondaryContainer" to colors.secondaryContainer,
        "tertiary" to colors.tertiary,
        "tertiaryContainer" to colors.tertiaryContainer,
        "error" to colors.error,
        "surface" to colors.surface,
        "surfaceContainer" to colors.surfaceContainer,
        "surfaceContainerHighest" to colors.surfaceContainerHighest,
        "onSurface" to colors.onSurface,
        "onSurfaceVariant" to colors.onSurfaceVariant,
        "outline" to colors.outline,
        "outlineVariant" to colors.outlineVariant,
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        roles.forEach { (name, color) -> Swatch(name, color) }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(modifier = Modifier.width(AtlasTheme.sizes.swatch * 2), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(AtlasTheme.sizes.swatch)
                .background(color, AtlasTheme.shapes.medium),
        )
        AtlasText(text = name, style = AtlasTheme.typography.labelSmall, color = AtlasTheme.colors.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun TypographySamples() {
    val typography = AtlasTheme.typography
    Column(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.xs),
    ) {
        AtlasText(text = "Заголовок темы", style = typography.emphasized.headlineMedium)
        AtlasText(text = "Заголовок экрана", style = typography.headlineSmall)
        AtlasText(text = "Раздел", style = typography.emphasized.titleLarge)
        AtlasText(text = "Подраздел", style = typography.titleMedium)
        AtlasText(text = "Основной текст страницы темы.", style = typography.bodyLarge)
        AtlasText(text = "Вспомогательный текст.", style = typography.bodyMedium, color = AtlasTheme.colors.onSurfaceVariant)
        AtlasText(text = "Подпись кнопки", style = typography.labelLarge)
        AtlasText(text = "val code = \"моноширинный\"", style = typography.code)
    }
}

@Composable
private fun ShapeSamples() {
    val shapes = AtlasTheme.shapes
    val samples = listOf(
        "extraSmall" to shapes.extraSmall,
        "small" to shapes.small,
        "medium" to shapes.medium,
        "large" to shapes.large,
        "extraLarge" to shapes.extraLarge,
        "full" to shapes.full,
        "cookie" to shapes.cookie,
        "clover" to shapes.clover,
        "sunny" to shapes.sunny,
        "softBurst" to shapes.softBurst,
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        samples.forEach { (name, shape) -> ShapeSample(name, shape) }
    }
}

@Composable
private fun ShapeSample(name: String, shape: Shape) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(AtlasTheme.sizes.shapeSample).background(AtlasTheme.colors.primaryContainer, shape))
        AtlasText(text = name, style = AtlasTheme.typography.labelSmall, color = AtlasTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun IconSamples() {
    val icons = listOf(
        AtlasIcons.Check, AtlasIcons.CheckCircle, AtlasIcons.ChevronRight, AtlasIcons.ArrowBack,
        AtlasIcons.Settings, AtlasIcons.Info, AtlasIcons.Close, AtlasIcons.Error, AtlasIcons.Refresh,
        AtlasIcons.LightMode, AtlasIcons.DarkMode, AtlasIcons.Contrast, AtlasIcons.Palette, AtlasIcons.Add,
        AtlasIcons.Folder, AtlasIcons.Article, AtlasIcons.BookmarkAdd, AtlasIcons.BookmarkAdded, AtlasIcons.Bookmarks,
        AtlasIcons.Edit, AtlasIcons.OpenInNew, AtlasIcons.ContentCopy, AtlasIcons.Download, AtlasIcons.DeleteSweep,
        AtlasIcons.MoreVert, AtlasIcons.PictureAsPdf, AtlasIcons.Share, AtlasIcons.Public, AtlasIcons.Code,
        AtlasIcons.Policy, AtlasIcons.SystemUpdate,
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        icons.forEach { AtlasIcon(icon = it, contentDescription = it.name) }
        AtlasIcon(icon = AtlasIcons.CheckCircle, contentDescription = null, tint = AtlasTheme.colors.primary)
        AtlasIconButton(icon = AtlasIcons.Settings, contentDescription = "Настройки", onClick = {})
    }
}

@Composable
private fun ListRowSamples() {
    AtlasListGroup {
        AtlasListRow(title = "Страница", leadingIcon = AtlasIcons.Article, index = 0, count = 4, onClick = {})
        AtlasListRow(
            title = "Просмотренная страница",
            leadingIcon = AtlasIcons.Article,
            checked = true,
            index = 1,
            count = 4,
            onClick = {},
        )
        AtlasListRow(
            title = "Раздел",
            subtitle = "3 из 7",
            leadingIcon = AtlasIcons.Folder,
            showChevron = true,
            index = 2,
            count = 4,
            onClick = {},
        )
        AtlasListRow(
            title = "Пройденный раздел",
            subtitle = "7 из 7",
            leadingIcon = AtlasIcons.Folder,
            checked = true,
            showChevron = true,
            index = 3,
            count = 4,
            onClick = {},
        )
    }
    AtlasListGroup {
        AtlasListRow(title = "С иконкой и подписью", subtitle = "Подпись строки", leadingIcon = AtlasIcons.Settings, onClick = {})
    }
    AtlasListGroup {
        AtlasListRow(
            title = "С действием",
            subtitle = "Кнопка в конце строки",
            leadingIcon = AtlasIcons.Article,
            trailingAction = AtlasRowAction(AtlasIcons.Edit, "Изменить", onClick = {}),
            onClick = {},
        )
    }
}

@Composable
private fun ReorderSamples() {
    var rows by remember { mutableStateOf(listOf("Первая", "Вторая", "Третья", "Четвёртая")) }
    AtlasReorderableGroup(
        items = rows,
        itemKey = { it },
        onMove = { from, to -> rows = rows.toMutableList().apply { add(to, removeAt(from)) } },
    ) { title, index, count, rowModifier ->
        AtlasListRow(
            title = title,
            modifier = rowModifier,
            leadingIcon = AtlasIcons.Bookmarks,
            index = index,
            count = count,
            onClick = {},
        )
    }
}

@Composable
private fun ButtonSamples() {
    Column(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasPrimaryButton(text = "Дальше", onClick = {}, modifier = Modifier.fillMaxWidth())
        AtlasPrimaryButton(text = "Недоступно", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        AtlasSecondaryButton(text = "Скачать заново", onClick = {}, modifier = Modifier.fillMaxWidth())
        AtlasTextButton(text = "Подробнее", onClick = {})
    }
}

@Composable
private fun ChoiceSamples() {
    var selected by remember { mutableStateOf("system") }
    AtlasChoiceGroup(
        options = listOf(
            AtlasChoiceOption("light", "Светлая", AtlasIcons.LightMode),
            AtlasChoiceOption("dark", "Тёмная", AtlasIcons.DarkMode),
            AtlasChoiceOption("system", "Система", AtlasIcons.Contrast),
        ),
        selected = selected,
        onSelect = { selected = it },
    )
}

@Composable
private fun ToggleSamples() {
    var checked by remember { mutableStateOf(true) }
    AtlasListGroup {
        AtlasSwitchRow(title = "Просмотрено", checked = checked, index = 0, count = 2, onCheckedChange = { checked = it })
        AtlasSwitchRow(title = "Выключено", subtitle = "Подпись переключателя", checked = false, index = 1, count = 2, onCheckedChange = {})
    }
}

@Composable
private fun TextFieldSamples() {
    var address by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasTextField(
            value = address,
            onValueChange = { address = it },
            label = "Ссылка на тему",
            supportingText = "Папка Яндекс.Диска, GitHub или файл .zip",
        )
        AtlasTextField(
            value = "http://example.com",
            onValueChange = {},
            label = "С ошибкой",
            supportingText = "Нужен https://",
            isError = true,
        )
    }
}

@Composable
private fun ProgressSamples() {
    Column(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasText(text = "Пройдено 12 из 30", style = AtlasTheme.typography.bodyMedium)
        AtlasProgressBar(progress = 0.4f)
        AtlasText(text = "Размер неизвестен", style = AtlasTheme.typography.bodyMedium)
        AtlasProgressBar(progress = null)
    }
}

@Composable
private fun DividerSample() {
    Column(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        AtlasText(text = "Абзац до разделителя")
        AtlasDivider()
        AtlasText(text = "Абзац после разделителя")
    }
}

@Composable
private fun OverlaySamples() {
    var sheetShown by remember { mutableStateOf(false) }
    var dialogShown by remember { mutableStateOf(false) }
    var confirmShown by remember { mutableStateOf(false) }
    var options by remember {
        mutableStateOf(
            listOf(
                AtlasCheckOption(key = "delete", title = "Удалить тему", checked = false),
                AtlasCheckOption(key = "reset", title = "Сбросить прогресс", checked = false),
            ),
        )
    }
    Row(
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
    ) {
        AtlasSecondaryButton(text = "Шторка", onClick = { sheetShown = true })
        AtlasSecondaryButton(text = "Диалог", onClick = { dialogShown = true })
        AtlasSecondaryButton(text = "Вопрос", onClick = { confirmShown = true })
    }
    if (confirmShown) {
        AtlasConfirmDialog(
            title = "Тема уже установлена",
            text = "Заменить источник и содержимое?",
            confirmText = "Заменить",
            onConfirm = { confirmShown = false },
            onDismiss = { confirmShown = false },
        )
    }
    if (sheetShown) {
        AtlasBottomSheet(title = "Что будет удалено", onDismiss = { sheetShown = false }) {
            AtlasText(text = "Тема и её файлы. Прогресс останется.", color = AtlasTheme.colors.onSurfaceVariant)
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
        AtlasLoading(text = "Загружаю тему")
    }
    AtlasLoadingIndicator(modifier = Modifier.fillMaxWidth())
    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        AtlasError(message = "Не удалось загрузить тему", actionText = "Повторить")
    }
}

@Preview
@Composable
private fun ComponentCatalogLightPreview() {
    AtlasTheme(darkTheme = false, wallpaperColors = false) {
        CatalogSection(themeName = "Светлая тема")
    }
}

@Preview
@Composable
private fun ComponentCatalogDarkPreview() {
    AtlasTheme(darkTheme = true, wallpaperColors = false) {
        CatalogSection(themeName = "Тёмная тема")
    }
}
