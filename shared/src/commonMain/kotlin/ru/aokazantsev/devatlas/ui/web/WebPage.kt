package ru.aokazantsev.devatlas.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest

@Composable
expect fun WebPage(
    startUrl: String,
    state: WebPageState,
    onPdf: (PdfRequest) -> Unit,
    onExternal: (String) -> Unit,
    onNothingShown: () -> Unit,
    modifier: Modifier = Modifier,
)
