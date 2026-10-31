package ru.aokazantsev.devatlas.files.domain.model

import okio.Sink

interface PendingPublicFile {
    val uri: String

    fun sink(): Sink

    fun publish(): String?

    fun discard()
}
