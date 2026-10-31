package ru.aokazantsev.devatlas.files.domain.model

data class PdfRequest(
    val url: String,
    val fileName: String? = null,
    val title: String? = null,
    val contentDisposition: String? = null,
)
