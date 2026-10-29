package ru.aokazantsev.devatlas.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun OnboardingScreen(onNext: () -> Unit) {
    AtlasScaffold(topBar = { AtlasTopBar(title = "DevAtlas") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AtlasTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
        ) {
            AtlasText(text = "Добро пожаловать", style = AtlasTheme.typography.title)
            AtlasText(
                text = "Здесь будут согласие с политикой данных и выбор темы.",
                color = AtlasTheme.colors.onSurfaceMuted,
                modifier = Modifier.weight(1f),
            )
            AtlasPrimaryButton(text = "Дальше", onClick = onNext, modifier = Modifier.fillMaxWidth())
        }
    }
}
