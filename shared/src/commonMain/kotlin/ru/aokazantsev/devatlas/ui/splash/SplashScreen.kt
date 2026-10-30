package ru.aokazantsev.devatlas.ui.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton

@Composable
fun SplashScreen(
    state: SplashState,
    onRetry: () -> Unit,
    onRedownload: () -> Unit,
    onChooseAnother: () -> Unit,
) {
    AtlasScaffold { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (state) {
            is SplashState.Failed -> AtlasError(
                message = "Не удалось открыть данные приложения: ${state.reason}",
                actionText = "Повторить",
                onAction = onRetry,
                modifier = modifier,
            )
            is SplashState.ThemeDamaged -> DamagedTheme(state, onRedownload, onChooseAnother, modifier)
            else -> AtlasLoading(modifier = modifier)
        }
    }
}

@Composable
private fun DamagedTheme(state: SplashState.ThemeDamaged, onRedownload: () -> Unit, onChooseAnother: () -> Unit, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    val busyText = state.busyText
    if (busyText != null) {
        AtlasLoading(text = busyText, modifier = modifier)
        return
    }
    val failure = state.failure
    val message = buildString {
        append("Тема «${state.theme.title}» повреждена: ${state.theme.reason}. Прогресс не тронут.")
        append("\n\n«Выбрать другую тему» уберёт эту тему с телефона, отметки «Просмотрено» и закладки сохранятся.")
        if (failure != null) append("\n\nНе получилось: ${failure.message}")
    }
    AtlasError(
        message = message,
        actionText = "Скачать заново",
        onAction = onRedownload,
        modifier = modifier,
        extraActions = {
            AtlasSecondaryButton(text = "Выбрать другую тему", onClick = onChooseAnother)
            failure?.helpUrl?.let { url -> AtlasTextButton(text = "Что исправить", onClick = { uriHandler.openUri(url) }) }
        },
    )
}
