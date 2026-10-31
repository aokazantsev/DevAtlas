package ru.aokazantsev.devatlas.files.data

import okio.IOException

internal class PublicFileWriteException(cause: IOException) : Exception(cause)
