package ru.aokazantsev.devatlas.ui.themes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.ui.format.formatSize

@Composable
fun ThemeDetailsScreen(
    viewModel: ThemeDetailsViewModel,
    onBack: () -> Unit,
    onApplied: () -> Unit,
    onManage: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateState by viewModel.update.state.collectAsStateWithLifecycle()
    val shown = state
    val theme = shown?.theme
    AtlasScaffold(topBar = { AtlasTopBar(title = theme?.title.orEmpty(), onBack = onBack) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when {
            theme == null -> AtlasLoading(modifier = modifier)
            updateState == UpdateState.Checking -> AtlasLoading(text = "Проверяю обновление…", modifier = modifier)
            updateState == UpdateState.Applying -> AtlasLoading(text = "Обновляю тему…", modifier = modifier)
            else -> Column(
                modifier = modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
            ) {
                theme.description?.let {
                    AtlasText(
                        text = it,
                        color = AtlasTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
                    )
                }
                AtlasListGroup {
                    AtlasListRow(title = "Страниц", counter = theme.pageCount.toString(), index = 0, count = 3)
                    AtlasListRow(title = "Размер", counter = formatSize(theme.sizeBytes), index = 1, count = 3)
                    AtlasListRow(title = "Источник", subtitle = theme.sourceUrl, index = 2, count = 3)
                }
                if (updateState == UpdateState.Idle) {
                    Column(
                        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
                        verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.s),
                    ) {
                        shown.applyFailure?.let { AtlasText(text = it, color = AtlasTheme.colors.error) }
                        if (shown.isCurrent) {
                            AtlasText(text = "Это текущая тема", color = AtlasTheme.colors.primary)
                        } else {
                            AtlasPrimaryButton(
                                text = "Применить",
                                onClick = { viewModel.apply(onApplied) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        AtlasSecondaryButton(text = "Обновить", onClick = { viewModel.update.check() }, modifier = Modifier.fillMaxWidth())
                        AtlasTextButton(text = "Управлять", onClick = onManage)
                    }
                }
            }
        }
    }
    UpdateHost(state = updateState, controller = viewModel.update)
}
