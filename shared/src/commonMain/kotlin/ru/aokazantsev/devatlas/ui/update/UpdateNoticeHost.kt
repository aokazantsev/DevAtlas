package ru.aokazantsev.devatlas.ui.update

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun UpdateNoticeHost(viewModel: UpdateNoticeViewModel, onOpenUpdate: () -> Unit) {
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val shown = notice ?: return
    AtlasBottomSheet(title = "Доступна версия ${shown.latest.version}", onDismiss = viewModel::dismiss) {
        AtlasText(text = "Посмотрите, что нового, и обновите приложение", color = AtlasTheme.colors.onSurfaceVariant)
        AtlasPrimaryButton(
            text = "Что нового",
            onClick = {
                viewModel.dismiss()
                onOpenUpdate()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AtlasSecondaryButton(text = "Позже", onClick = viewModel::dismiss, modifier = Modifier.fillMaxWidth())
    }
}
