package ru.aokazantsev.devatlas.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasCheckboxRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onNext: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf<ConsentDetails?>(null) }
    AtlasScaffold(
        topBar = { AtlasTopBar(title = "DevAtlas") },
        bottomBar = {
            AtlasPrimaryButton(
                text = "Дальше",
                onClick = { viewModel.next(onNext) },
                enabled = state.policyAccepted,
                modifier = Modifier.fillMaxWidth().padding(AtlasTheme.spacing.l),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.l),
        ) {
            Welcome()
            PolicyConsent(state = state, viewModel = viewModel)
            ReportOptions(state = state, viewModel = viewModel, onShowDetails = { details = it })
        }
    }
    details?.let { shown ->
        AtlasBottomSheet(title = shown.title, onDismiss = { details = null }) {
            AtlasText(text = shown.text, color = AtlasTheme.colors.onSurfaceVariant)
            AtlasTextButton(text = "Закрыть", onClick = { details = null })
        }
    }
}

@Composable
private fun Welcome() {
    AtlasText(
        text = "Добро пожаловать",
        style = AtlasTheme.typography.emphasized.headlineMedium,
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
    )
    AtlasText(
        text = "Темы с разделами, страницами и схемами. Тему вы добавляете сами ссылкой, дальше приложение работает без сети.",
        color = AtlasTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.l),
    )
}

@Composable
private fun PolicyConsent(state: OnboardingState, viewModel: OnboardingViewModel) {
    val uriHandler = LocalUriHandler.current
    AtlasListGroup {
        AtlasCheckboxRow(
            title = "Принимаю политику данных",
            subtitle = "Версия ${Consent.CURRENT_POLICY_VERSION}",
            checked = state.policyAccepted,
            onCheckedChange = viewModel::setPolicyAccepted,
        )
    }
    AtlasTextButton(
        text = "Читать политику",
        onClick = { uriHandler.openUri(Consent.POLICY_URL) },
        modifier = Modifier.padding(horizontal = AtlasTheme.spacing.s),
    )
}

@Composable
private fun ReportOptions(state: OnboardingState, viewModel: OnboardingViewModel, onShowDetails: (ConsentDetails) -> Unit) {
    AtlasListGroup {
        AtlasCheckboxRow(
            title = "Отправлять отчёты о сбоях",
            checked = state.crashReports,
            onCheckedChange = viewModel::setCrashReports,
            index = 0,
            count = 2,
        )
        AtlasCheckboxRow(
            title = "Отправлять статистику",
            checked = state.usageStatistics,
            onCheckedChange = viewModel::setUsageStatistics,
            index = 1,
            count = 2,
        )
    }
    Column(modifier = Modifier.padding(horizontal = AtlasTheme.spacing.s)) {
        AtlasTextButton(text = "Что уходит в отчёте о сбое", onClick = { onShowDetails(ConsentDetails.CrashReport) })
        AtlasTextButton(text = "Что уходит в статистике", onClick = { onShowDetails(ConsentDetails.Statistics) })
    }
}
