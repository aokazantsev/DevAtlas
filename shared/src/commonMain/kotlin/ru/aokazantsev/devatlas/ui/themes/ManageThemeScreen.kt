package ru.aokazantsev.devatlas.ui.themes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasCheckboxRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextField
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval

@Composable
fun ManageThemeScreen(
    viewModel: ManageThemeViewModel,
    onBack: () -> Unit,
    onRemoved: (ThemeRemoval) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateState by viewModel.update.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.removals.collect(onRemoved) }
    LaunchedEffect(state.loaded, state.theme, state.busy) {
        if (state.loaded && state.theme == null && !state.busy) onBack()
    }
    AtlasScaffold(topBar = { AtlasTopBar(title = "Управление темой", onBack = onBack) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when {
            state.theme == null || state.busy -> AtlasLoading(modifier = modifier)
            updateState == UpdateState.Checking -> AtlasLoading(text = "Проверяю ссылку…", modifier = modifier)
            updateState == UpdateState.Applying -> AtlasLoading(text = "Обновляю тему…", modifier = modifier)
            else -> ManageContent(state = state, viewModel = viewModel, modifier = modifier)
        }
    }
    UpdateHost(state = updateState, controller = viewModel.update)
    state.confirm?.let { confirm ->
        AtlasBottomSheet(title = "Что будет удалено", onDismiss = viewModel::cancelRemoval) {
            AtlasText(text = confirmText(confirm), color = AtlasTheme.colors.onSurfaceVariant)
            AtlasPrimaryButton(text = "Подтвердить", onClick = viewModel::confirmRemoval, modifier = Modifier.fillMaxWidth())
            AtlasSecondaryButton(text = "Отмена", onClick = viewModel::cancelRemoval, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ManageContent(state: ManageState, viewModel: ManageThemeViewModel, modifier: Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        ) {
            AtlasText(text = "Ссылка источника", style = AtlasTheme.typography.emphasized.titleMedium)
            AtlasTextField(
                value = state.sourceAddress,
                onValueChange = viewModel::setSourceAddress,
                label = "Ссылка на тему",
                supportingText = "Тема по новой ссылке должна иметь тот же id, иначе замена отклонится",
            )
            AtlasSecondaryButton(
                text = "Проверить и сохранить ссылку",
                onClick = viewModel::saveSource,
                enabled = state.sourceAddress.isNotBlank() && state.sourceAddress != state.theme?.sourceUrl,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AtlasListGroup {
            AtlasCheckboxRow(
                title = "Удалить тему",
                checked = state.deleteTheme,
                onCheckedChange = viewModel::setDeleteTheme,
                index = 0,
                count = 2,
            )
            AtlasCheckboxRow(
                title = "Сбросить прогресс",
                checked = state.resetProgress,
                onCheckedChange = viewModel::setResetProgress,
                index = 1,
                count = 2,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        ) {
            state.notice?.let { AtlasText(text = it, color = AtlasTheme.colors.primary) }
            AtlasPrimaryButton(
                text = "Продолжить",
                onClick = viewModel::requestRemoval,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun confirmText(confirm: RemovalConfirm): String = buildString {
    if (confirm.deleteTheme) {
        append("Тема «${confirm.themeTitle}» и её файлы удалятся с телефона.")
        if (confirm.isCurrent) append(" Откроется последняя использованная тема, а если тем не останется — первый экран приложения.")
    }
    if (confirm.resetProgress) {
        if (isNotEmpty()) append("\n\n")
        append("Отметки «Просмотрено» (${confirm.viewedCount}) удалятся.")
    } else if (confirm.viewedCount > 0) {
        append("\n\nОтметки «Просмотрено» (${confirm.viewedCount}) останутся ")
        append("и вернутся у страниц, которые будут в теме, если вы снова её добавите.")
    }
}
