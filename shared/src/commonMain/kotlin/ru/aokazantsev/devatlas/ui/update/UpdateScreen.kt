package ru.aokazantsev.devatlas.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasProgressBar
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.ui.format.formatSize
import ru.aokazantsev.devatlas.ui.page.code.CodeCopy
import ru.aokazantsev.devatlas.ui.page.content.PageBlocks
import ru.aokazantsev.devatlas.ui.page.content.PageLinks

@Composable
fun UpdateScreen(viewModel: UpdateViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    AtlasScaffold(topBar = { AtlasTopBar(title = "Обновление", onBack = onBack) }) { padding ->
        val modifier = Modifier.padding(padding)
        when (val shown = state) {
            UpdateUiState.Checking -> AtlasLoading(modifier = modifier, text = "Проверяю, вышла ли новая версия…")
            is UpdateUiState.Failed -> AtlasError(
                message = shown.message,
                modifier = modifier,
                actionText = "Повторить",
                onAction = viewModel::check,
            )
            is UpdateUiState.UpToDate -> UpToDate(shown.version, viewModel, modifier)
            is UpdateUiState.Available -> Available(shown, viewModel, modifier)
        }
    }
}

@Composable
private fun UpToDate(version: String, viewModel: UpdateViewModel, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        AtlasText(text = "Установлена последняя версия $version", style = AtlasTheme.typography.titleMedium)
        AtlasSecondaryButton(text = "Проверить ещё раз", onClick = viewModel::check)
    }
}

@Composable
private fun Available(state: UpdateUiState.Available, viewModel: UpdateViewModel, modifier: Modifier) {
    val links = remember(viewModel) {
        PageLinks(
            onClick = { link -> (link.target as? LinkTarget.Web)?.let { viewModel.openLink(it.url) } },
            onLongPress = {},
        )
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(AtlasTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
    ) {
        AtlasText(
            text = "Доступна версия ${state.release.version}",
            style = AtlasTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        UpdateActions(state.step, viewModel)
        state.offlineNote?.let { AtlasText(text = it, color = AtlasTheme.colors.onSurfaceVariant) }
        AtlasText(text = "Что нового", style = AtlasTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        state.notes.forEach { notes ->
            AtlasText(
                text = "Версия ${notes.version}",
                style = AtlasTheme.typography.titleMedium,
                color = AtlasTheme.colors.primary,
                modifier = Modifier.semantics { heading() },
            )
            if (notes.blocks.isEmpty()) {
                AtlasText(text = "Без описания", color = AtlasTheme.colors.onSurfaceVariant)
            } else {
                PageBlocks(
                    blocks = notes.blocks,
                    diagrams = emptyMap(),
                    links = links,
                    onOpenDiagram = {},
                    codeCopy = CodeCopy(copy = null, systemConfirms = false),
                )
            }
        }
        if (state.hasOlderNotes) {
            AtlasTextButton(text = "Более ранние версии — на GitHub", onClick = viewModel::openHistory)
        }
    }
}

@Composable
private fun UpdateActions(step: UpdateStep, viewModel: UpdateViewModel) {
    val wide = Modifier.fillMaxWidth()
    when (step) {
        UpdateStep.Idle -> AtlasPrimaryButton(text = "Обновить", onClick = viewModel::update, modifier = wide)
        is UpdateStep.Downloading -> {
            val total = step.totalBytes?.takeIf { it > 0L }
            AtlasProgressBar(progress = total?.let { (step.receivedBytes.toFloat() / it).coerceIn(0f, 1f) })
            val received = formatSize(step.receivedBytes)
            AtlasText(
                text = if (total == null) "Скачано $received" else "Скачано $received из ${formatSize(total)}",
                color = AtlasTheme.colors.onSurfaceVariant,
            )
            AtlasSecondaryButton(text = "Отмена", onClick = viewModel::cancelDownload, modifier = wide)
        }
        UpdateStep.NeedsPermission -> {
            AtlasText(text = "Чтобы поставить новую версию, разрешите DevAtlas устанавливать приложения и вернитесь сюда")
            AtlasPrimaryButton(text = "Разрешить установку", onClick = viewModel::allowInstall, modifier = wide)
        }
        UpdateStep.Installing -> {
            AtlasText(text = "Файл скачан и сверен с GitHub. Подтвердите установку в окне Android")
            AtlasSecondaryButton(text = "Установить ещё раз", onClick = viewModel::update, modifier = wide)
        }
        is UpdateStep.Failed -> {
            AtlasText(text = step.message, color = AtlasTheme.colors.error)
            if (step.retryable) {
                AtlasPrimaryButton(text = "Повторить", onClick = viewModel::update, modifier = wide)
            } else {
                AtlasPrimaryButton(text = "Открыть страницу выпусков", onClick = viewModel::openHistory, modifier = wide)
            }
        }
    }
}
