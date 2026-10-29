package ru.aokazantsev.devatlas.ui.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoading
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold

@Composable
fun SplashScreen(state: SplashState, onRetry: () -> Unit) {
    AtlasScaffold { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        if (state is SplashState.Failed) {
            AtlasError(
                message = "Не удалось открыть данные приложения: ${state.reason}",
                actionText = "Повторить",
                onAction = onRetry,
                modifier = modifier,
            )
        } else {
            AtlasLoading(modifier = modifier)
        }
    }
}
