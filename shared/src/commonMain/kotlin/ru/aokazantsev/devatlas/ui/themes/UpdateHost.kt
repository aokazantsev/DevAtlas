package ru.aokazantsev.devatlas.ui.themes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun UpdateHost(state: UpdateState, controller: ThemeUpdateController) {
    val uriHandler = LocalUriHandler.current
    when (state) {
        UpdateState.Idle, UpdateState.Checking, UpdateState.Applying -> Unit
        is UpdateState.Message -> AtlasBottomSheet(title = state.text, onDismiss = controller::dismiss) {
            AtlasSecondaryButton(text = "Закрыть", onClick = controller::dismiss, modifier = Modifier.fillMaxWidth())
        }
        is UpdateState.Failed -> AtlasBottomSheet(title = "Не удалось обновить", onDismiss = controller::dismiss) {
            AtlasText(text = state.failure.message, color = AtlasTheme.colors.onSurfaceVariant)
            state.failure.helpUrl?.let { url ->
                AtlasTextButton(text = "Что исправить", onClick = { uriHandler.openUri(url) })
            }
            AtlasSecondaryButton(text = "Закрыть", onClick = controller::dismiss, modifier = Modifier.fillMaxWidth())
        }
        is UpdateState.Available -> AtlasBottomSheet(title = "Есть обновление", onDismiss = controller::cancel) {
            AtlasText(text = summaryText(state), color = AtlasTheme.colors.onSurfaceVariant)
            AtlasPrimaryButton(text = "Обновить", onClick = controller::confirm, modifier = Modifier.fillMaxWidth())
            AtlasSecondaryButton(text = "Отмена", onClick = controller::cancel, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun summaryText(state: UpdateState.Available): String {
    val summary = state.summary
    val source = if (state.sourceChanged) "Ссылка источника будет заменена. " else ""
    val changes = when {
        summary.isEmpty && !state.contentChanged -> "Содержимое не изменилось."
        summary.isEmpty -> "Страницы не изменились: обновились схемы или другие файлы темы."
        else ->
            "Страниц добавлено: ${summary.added}, удалено: ${summary.removed}, изменено: ${summary.changed}. " +
                "Отметки «Просмотрено» у удалённых страниц сбросятся."
    }
    return source + changes
}
