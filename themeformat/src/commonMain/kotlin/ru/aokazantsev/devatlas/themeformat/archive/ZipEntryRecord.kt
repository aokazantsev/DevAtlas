package ru.aokazantsev.devatlas.themeformat.archive

internal data class ZipEntryRecord(
    val name: String,
    val method: Int,
    val encrypted: Boolean,
    val symlink: Boolean,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long,
) {
    val isDirectory: Boolean
        get() = name.endsWith("/")
}
