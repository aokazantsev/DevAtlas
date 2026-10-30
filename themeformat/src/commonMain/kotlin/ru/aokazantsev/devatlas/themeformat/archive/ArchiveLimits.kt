package ru.aokazantsev.devatlas.themeformat.archive

data class ArchiveLimits(
    val maxArchiveBytes: Long,
    val maxEntries: Int,
    val maxRawEntries: Int,
    val maxFileBytes: Long,
    val maxTotalBytes: Long,
    val maxDepth: Int,
    val maxPathLength: Int,
) {
    companion object {
        const val MEGABYTE = 1024L * 1024L

        val Default = ArchiveLimits(
            maxArchiveBytes = 50 * MEGABYTE,
            maxEntries = 2_000,
            maxRawEntries = 20_000,
            maxFileBytes = MEGABYTE,
            maxTotalBytes = 100 * MEGABYTE,
            maxDepth = 8,
            maxPathLength = 200,
        )
    }
}
