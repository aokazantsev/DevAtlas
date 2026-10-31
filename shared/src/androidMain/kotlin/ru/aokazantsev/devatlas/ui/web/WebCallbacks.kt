package ru.aokazantsev.devatlas.ui.web

import ru.aokazantsev.devatlas.files.domain.model.PdfRequest

internal class WebCallbacks(
    val onPdf: (PdfRequest) -> Unit,
    val onExternal: (String) -> Unit,
    val onNothingShown: () -> Unit,
)
