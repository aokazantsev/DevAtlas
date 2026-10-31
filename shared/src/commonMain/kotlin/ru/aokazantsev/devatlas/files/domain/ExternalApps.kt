package ru.aokazantsev.devatlas.files.domain

interface ExternalApps {
    fun openUri(uri: String): Boolean

    fun openFile(uri: String, mimeType: String): Boolean

    fun shareFile(uri: String, mimeType: String): Boolean

    fun shareText(text: String): Boolean
}
