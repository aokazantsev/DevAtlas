package ru.aokazantsev.devatlas.ui.page

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasBottomSheet
import ru.aokazantsev.devatlas.designsystem.component.AtlasPrimaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasSecondaryButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasText
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.ui.links.LinkActions

@Composable
internal fun LinkMenuSheet(link: PageInline.Link?, pageTitle: String, actions: LinkActions, onDismiss: () -> Unit) {
    val uri = link?.externalUri() ?: return
    val pdf = link.pdfRequest(pageTitle)
    AtlasBottomSheet(title = "Ссылка", onDismiss = onDismiss) {
        AtlasText(
            text = uri,
            style = AtlasTheme.typography.bodyMedium,
            color = AtlasTheme.colors.onSurfaceVariant,
            maxLines = SHOWN_URI_LINES,
        )
        AtlasPrimaryButton(
            text = "Копировать ссылку",
            onClick = {
                actions.copyLink(uri)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (pdf != null) {
            AtlasSecondaryButton(
                text = "Скачать заново",
                onClick = {
                    onDismiss()
                    actions.redownloadPdf(pdf)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val SHOWN_URI_LINES = 3
