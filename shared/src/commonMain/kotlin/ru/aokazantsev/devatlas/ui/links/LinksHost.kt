package ru.aokazantsev.devatlas.ui.links

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasProgressBar
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.files.domain.model.PdfState
import ru.aokazantsev.devatlas.ui.format.formatSize

@Composable
fun LinksHost(viewModel: LinksViewModel) {
    val pdf by viewModel.pdfState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    when (val shown = pdf) {
        PdfState.Idle -> Unit
        is PdfState.Downloading -> AtlasBottomSheet(title = "Скачиваю PDF", onDismiss = viewModel::cancelPdf) {
            val total = shown.totalBytes?.takeIf { it > 0L }
            AtlasProgressBar(progress = total?.let { (shown.receivedBytes.toFloat() / it).coerceIn(0f, 1f) })
            val received = formatSize(shown.receivedBytes)
            AtlasText(
                text = if (total == null) "Скачано $received" else "$received из ${formatSize(total)}",
                color = AtlasTheme.colors.onSurfaceVariant,
            )
            AtlasSecondaryButton(text = "Отмена", onClick = viewModel::cancelPdf, modifier = Modifier.fillMaxWidth())
        }
        is PdfState.Failed -> AtlasBottomSheet(title = "PDF не скачался", onDismiss = viewModel::dismissPdf) {
            AtlasText(text = shown.message)
            AtlasPrimaryButton(text = "Повторить", onClick = { viewModel.retryPdf(shown) }, modifier = Modifier.fillMaxWidth())
            AtlasSecondaryButton(text = "Закрыть", onClick = viewModel::dismissPdf, modifier = Modifier.fillMaxWidth())
        }
        is PdfState.NoViewer -> AtlasBottomSheet(title = "Нет программы для PDF", onDismiss = viewModel::dismissPdf) {
            AtlasText(
                text = "Файл «${shown.fileName}» сохранён в Загрузки/DevAtlas. Установите программу для просмотра PDF — " +
                    "файл откроется из «Скачанных файлов» в настройках.",
            )
            AtlasPrimaryButton(text = "Понятно", onClick = viewModel::dismissPdf, modifier = Modifier.fillMaxWidth())
        }
    }
    val message = notice ?: return
    AtlasBottomSheet(title = "Ссылка не открылась", onDismiss = viewModel::dismissNotice) {
        AtlasText(text = message)
        AtlasPrimaryButton(text = "Понятно", onClick = viewModel::dismissNotice, modifier = Modifier.fillMaxWidth())
    }
}
