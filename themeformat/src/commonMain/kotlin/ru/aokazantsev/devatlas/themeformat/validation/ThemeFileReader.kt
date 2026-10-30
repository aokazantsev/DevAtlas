package ru.aokazantsev.devatlas.themeformat.validation

import okio.FileSystem
import okio.IOException
import okio.Path
import ru.aokazantsev.devatlas.themeformat.archive.ArchiveLimits
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue

internal class ThemeFileReader(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val limits: ArchiveLimits,
) {

    fun listFiles(): Set<String> = ThemeFolder.files(fileSystem, root)

    fun sizes(files: Set<String>): Map<String, Long> = files.associateWith { fileSystem.metadataOrNull(root / it)?.size ?: 0L }

    fun readText(file: String, issues: MutableList<ThemeIssue>): String? {
        val path = root / file
        return try {
            val size = fileSystem.metadata(path).size ?: 0L
            if (size > limits.maxFileBytes) return null
            val text = fileSystem.read(path) { readUtf8() }.removePrefix(BYTE_ORDER_MARK)
            if (REPLACEMENT_CHAR in text) {
                issues += error(file, IssueCode.ENCODING, "Файл не в кодировке UTF-8: пересохраните его в UTF-8")
                null
            } else {
                text
            }
        } catch (exception: IOException) {
            issues += error(file, IssueCode.FILE_UNSUPPORTED, "Файл не читается: ${exception.message.orEmpty()}")
            null
        }
    }

    private fun error(file: String, code: IssueCode, message: String) =
        ThemeIssue(severity = IssueSeverity.ERROR, code = code, message = message, file = file)

    private companion object {
        const val BYTE_ORDER_MARK = "\uFEFF"
        const val REPLACEMENT_CHAR = '\uFFFD'
    }
}
