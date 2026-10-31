package ru.aokazantsev.devatlas.ui.links

import ru.aokazantsev.devatlas.files.domain.model.PdfRequest

class LinkActions(
    val openPage: (String) -> Unit,
    val openWeb: (String) -> Unit,
    val openPdf: (PdfRequest) -> Unit,
    val redownloadPdf: (PdfRequest) -> Unit,
    val openExternal: (String) -> Unit,
    val copyLink: (String) -> Boolean,
)
