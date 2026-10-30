package ru.aokazantsev.devatlas.ui.addtheme

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasConfirmDialog
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextField
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeHelp
import ru.aokazantsev.devatlas.ui.format.formatSize

@Composable
fun AddThemeScreen(
    viewModel: AddThemeViewModel,
    onboarding: Boolean,
    onBack: (() -> Unit)?,
    onInstalled: (themeId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sampleAvailable by viewModel.sampleAvailable.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(viewModel) { viewModel.installed.collect(onInstalled) }
    AtlasScaffold(topBar = { AtlasTopBar(title = if (onboarding) "Первая тема" else "Добавить тему", onBack = onBack) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val current = state) {
            is AddThemeState.Editing -> EditingContent(
                state = current,
                showSample = onboarding || sampleAvailable,
                onboarding = onboarding,
                viewModel = viewModel,
                modifier = modifier,
            )
            is AddThemeState.Loading -> AtlasLoading(text = current.message, modifier = modifier)
            is AddThemeState.Preview -> PreviewContent(state = current, viewModel = viewModel, modifier = modifier)
            is AddThemeState.Failed -> FailureContent(state = current, viewModel = viewModel, modifier = modifier)
        }
    }
}

@Composable
private fun EditingContent(
    state: AddThemeState.Editing,
    showSample: Boolean,
    onboarding: Boolean,
    viewModel: AddThemeViewModel,
    modifier: Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
    ) {
        if (showSample) {
            AtlasListGroup {
                AtlasListRow(
                    title = "Начать с примера",
                    subtitle = "Короткая тема, которая показывает всё, что умеет приложение",
                    showChevron = true,
                    onClick = viewModel::startWithSample,
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        ) {
            if (onboarding) {
                AtlasText(text = "Добавить по ссылке", style = AtlasTheme.typography.emphasized.titleMedium)
            }
            AtlasTextField(
                value = state.address,
                onValueChange = viewModel::setAddress,
                label = "Ссылка на тему",
                supportingText = state.hint ?: "Папка или zip на Яндекс.Диске, GitHub или файл .zip",
            )
            AtlasSecondaryButton(text = "Вставить", onClick = viewModel::paste, modifier = Modifier.fillMaxWidth())
            AtlasPrimaryButton(
                text = "Проверить",
                onClick = { viewModel.load(state.address) },
                enabled = state.address.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            AtlasTextButton(text = "Как сделать тему", onClick = { uriHandler.openUri(ThemeHelp.PAGE_URL) })
        }
    }
}

@Composable
private fun PreviewContent(state: AddThemeState.Preview, viewModel: AddThemeViewModel, modifier: Modifier) {
    val manifest = state.prepared.manifest
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        ) {
            AtlasText(text = manifest.title, style = AtlasTheme.typography.emphasized.headlineMedium)
            manifest.description?.let { AtlasText(text = it, color = AtlasTheme.colors.onSurfaceVariant) }
        }
        AtlasListGroup {
            AtlasListRow(title = "Страниц", counter = manifest.pages.size.toString(), index = 0, count = 3)
            AtlasListRow(title = "Размер", counter = formatSize(state.prepared.sizeBytes), index = 1, count = 3)
            AtlasListRow(title = "Источник", subtitle = state.prepared.source, index = 2, count = 3)
        }
        if (state.existing != null) {
            AtlasText(
                text = "Эта тема уже установлена. Установка заменит источник и содержимое, " +
                    "отметки «Просмотрено» сохранятся у страниц, которые остались в теме.",
                color = AtlasTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
        ) {
            AtlasPrimaryButton(text = "Установить", onClick = viewModel::install, modifier = Modifier.fillMaxWidth())
            AtlasTextButton(text = "Отмена", onClick = viewModel::cancelPreview)
        }
    }
    if (state.confirmReplace) {
        AtlasConfirmDialog(
            title = "Тема уже установлена",
            text = "Заменить источник и содержимое темы «${manifest.title}»? " +
                "Отметки «Просмотрено» сохранятся у страниц, которые остались в теме.",
            confirmText = "Заменить",
            dismissText = "Не заменять",
            onConfirm = viewModel::install,
            onDismiss = viewModel::dismissReplace,
        )
    }
}

@Composable
private fun FailureContent(state: AddThemeState.Failed, viewModel: AddThemeViewModel, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    AtlasError(
        message = state.failure.message,
        actionText = "Повторить",
        onAction = { viewModel.load(state.address) },
        modifier = modifier,
        extraActions = {
            state.failure.helpUrl?.let { url ->
                AtlasTextButton(text = "Что исправить", onClick = { uriHandler.openUri(url) })
            }
            AtlasTextButton(text = "Изменить ссылку", onClick = viewModel::backToEditing)
        },
    )
}
