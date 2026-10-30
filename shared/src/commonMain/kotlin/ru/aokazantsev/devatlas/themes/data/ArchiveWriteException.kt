package ru.aokazantsev.devatlas.themes.data

import okio.IOException

internal class ArchiveWriteException(cause: IOException) : IOException(cause.message, cause)
