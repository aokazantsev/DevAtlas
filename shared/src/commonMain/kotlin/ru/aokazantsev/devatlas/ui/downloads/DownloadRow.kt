package ru.aokazantsev.devatlas.ui.downloads

import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile

data class DownloadRow(
    val file: DownloadedFile,
    val available: Boolean,
    val details: String,
)
