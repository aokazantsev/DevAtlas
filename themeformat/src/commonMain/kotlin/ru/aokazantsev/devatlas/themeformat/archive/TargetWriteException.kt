package ru.aokazantsev.devatlas.themeformat.archive

import okio.IOException

internal class TargetWriteException(cause: IOException) : IOException(cause.message, cause)
