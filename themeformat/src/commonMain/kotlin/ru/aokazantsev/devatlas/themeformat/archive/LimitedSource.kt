package ru.aokazantsev.devatlas.themeformat.archive

import okio.Buffer
import okio.ForwardingSource
import okio.Source

internal class LimitedSource(delegate: Source, private var remaining: Long) : ForwardingSource(delegate) {

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (remaining <= 0L) return -1L
        val read = super.read(sink, minOf(byteCount, remaining))
        if (read > 0) remaining -= read
        return read
    }
}
