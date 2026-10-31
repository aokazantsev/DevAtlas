package ru.aokazantsev.devatlas.files.domain

import ru.aokazantsev.devatlas.files.domain.model.PendingPublicFile

interface PublicFiles {
    val folderName: String

    fun create(name: String, mimeType: String): PendingPublicFile

    fun isReadable(uri: String): Boolean

    fun delete(uri: String): Boolean
}
