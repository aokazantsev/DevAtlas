package ru.aokazantsev.devatlas.ui.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun ContentsScreen(viewModel: ContentsViewModel, onOpenSettings: () -> Unit, onAddTheme: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = (state as? ContentsState.Ready)?.title ?: "Содержание"
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = title,
                actions = {
                    AtlasIconButton(icon = AtlasIcons.Settings, contentDescription = "Настройки", onClick = onOpenSettings)
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val shown = state) {
            ContentsState.Loading -> AtlasLoading(modifier = modifier)
            ContentsState.NoTheme -> AtlasError(
                message = "Тема не выбрана",
                actionText = "Добавить тему",
                onAction = onAddTheme,
                modifier = modifier,
            )
            is ContentsState.Ready -> Column(
                modifier = modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
            ) {
                shown.description?.let {
                    AtlasText(
                        text = it,
                        color = AtlasTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
                    )
                }
                AtlasListGroup {
                    shown.sections.forEachIndexed { index, section ->
                        AtlasListRow(title = section, index = index, count = shown.sections.size)
                    }
                }
            }
        }
    }
}
