package ru.aokazantsev.devatlas.files.domain

import kotlinx.coroutines.flow.StateFlow
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PdfState

interface PdfFiles {
    val state: StateFlow<PdfState>

    fun open(request: PdfRequest)

    fun redownload(request: PdfRequest)

    fun cancel()

    fun dismiss()
}
