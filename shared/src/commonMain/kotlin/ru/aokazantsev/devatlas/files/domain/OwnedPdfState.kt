package ru.aokazantsev.devatlas.files.domain

import kotlinx.coroutines.Job
import ru.aokazantsev.devatlas.files.domain.model.PdfState

internal data class OwnedPdfState(val owner: Job?, val state: PdfState)
