package ru.aokazantsev.devatlas.themeformat.archive

import okio.Buffer
import okio.BufferedSink
import okio.FileHandle
import okio.FileSystem
import okio.IOException
import okio.Inflater
import okio.InflaterSource
import okio.Path
import okio.Source
import okio.buffer
import okio.use
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueCollector
import ru.aokazantsev.devatlas.themeformat.path.ThemePaths

class ThemeArchiveExtractorImpl(
    private val limits: ArchiveLimits = ArchiveLimits.Default,
) : ThemeArchiveExtractor {

    override fun extract(fileSystem: FileSystem, archive: Path, target: Path, subFolder: String?): ExtractionResult {
        val collector = IssueCollector(archive.name)
        if (fileSystem.exists(target)) {
            collector.error(IssueCode.ARCHIVE_UNREADABLE, "Папка распаковки уже существует: нужна новая пустая папка")
            return ExtractionResult(files = emptyList(), issues = collector.issues)
        }
        val files = try {
            fileSystem.openReadOnly(archive).use { handle -> extract(fileSystem, handle, target, subFolder, collector) }
        } catch (error: TargetWriteException) {
            collector.error(IssueCode.ARCHIVE_UNREADABLE, "Не удалось записать распакованную тему: ${error.message ?: "не хватает места"}")
            emptyList()
        } catch (error: IOException) {
            collector.error(IssueCode.ARCHIVE_UNREADABLE, "Архив не читается: ${error.message ?: "ошибка ввода-вывода"}")
            emptyList()
        }
        if (collector.issues.isNotEmpty()) fileSystem.deleteRecursively(target, mustExist = false)
        return ExtractionResult(files = files, issues = collector.issues)
    }

    private fun extract(
        fileSystem: FileSystem,
        handle: FileHandle,
        target: Path,
        subFolder: String?,
        collector: IssueCollector,
    ): List<String> {
        val reader = ZipDirectoryReader(handle)
        val plan = plan(handle, reader, subFolder, collector) ?: return emptyList()
        target.parent?.let(fileSystem::createDirectories)
        fileSystem.createDirectory(target, mustCreate = true)
        var total = 0L
        return plan.entries.map { entry ->
            val relative = entry.name.removePrefix(plan.root)
            val destination = target / relative
            total += copy(fileSystem, reader, handle, entry, destination, limits.maxTotalBytes - total)
            relative
        }
    }

    private fun plan(handle: FileHandle, reader: ZipDirectoryReader, subFolder: String?, collector: IssueCollector): ExtractionPlan? {
        if (handle.size() > limits.maxArchiveBytes) {
            collector.error(IssueCode.ARCHIVE_LIMIT, "Архив больше ${limits.maxArchiveBytes / ArchiveLimits.MEGABYTE} МБ")
            return null
        }
        val selected = select(reader.entries(), subFolder, collector) ?: return null
        return selected.takeIf { checkLimits(it.entries, it.root, handle.size(), collector) }
    }

    private fun select(entries: List<ZipEntryRecord>, subFolder: String?, collector: IssueCollector): ExtractionPlan? {
        if (entries.size > limits.maxRawEntries) {
            collector.error(IssueCode.ARCHIVE_LIMIT, "В каталоге архива больше ${limits.maxRawEntries} записей")
            return null
        }
        val named = entries.filterNot { it.isDirectory }.map { it.copy(name = ArchiveEntryNames.withoutDotSegments(it.name)) }
        val candidates = named.map { it.name }.filter { ArchiveEntryNames.unsafeReason(it) == null && !ArchiveEntryNames.isIgnored(it) }
        val root = ThemeRootLocator.locate(candidates, subFolder)
        if (root == null) {
            val message = if (subFolder == null) {
                "Не найден manifest.json: он должен лежать в корне архива или в единственной папке внутри него"
            } else {
                "Не найден manifest.json в папке «$subFolder» архива"
            }
            collector.error(IssueCode.ARCHIVE_NO_ROOT, message)
            return null
        }
        val files = safeFiles(named.filter { it.name.startsWith(root) }, collector) ?: return null
        return ExtractionPlan(root, files)
    }

    private fun safeFiles(entries: List<ZipEntryRecord>, collector: IssueCollector): List<ZipEntryRecord>? {
        val seen = mutableSetOf<String>()
        val files = mutableListOf<ZipEntryRecord>()
        entries.forEach { entry ->
            val reason = ArchiveEntryNames.unsafeReason(entry.name)
            when {
                reason != null -> collector.error(IssueCode.ARCHIVE_UNSAFE_PATH, "«${entry.name}»: $reason")
                ArchiveEntryNames.isIgnored(entry.name) -> Unit
                entry.symlink -> collector.error(IssueCode.ARCHIVE_UNSAFE_PATH, "«${entry.name}»: символические ссылки запрещены")
                entry.encrypted -> collector.error(IssueCode.ARCHIVE_UNREADABLE, "«${entry.name}»: архив защищён паролем")
                entry.method != STORED && entry.method != DEFLATED ->
                    collector.error(IssueCode.ARCHIVE_UNREADABLE, "«${entry.name}»: метод сжатия не поддерживается, нужен обычный zip")
                !seen.add(entry.name.lowercase()) ->
                    collector.error(IssueCode.ARCHIVE_UNSAFE_PATH, "«${entry.name}»: файл с таким именем уже есть в архиве")
                else -> files += entry
            }
        }
        if (files.size > limits.maxEntries) {
            collector.error(IssueCode.ARCHIVE_LIMIT, "В архиве больше ${limits.maxEntries} файлов")
        }
        return files.takeIf { collector.issues.isEmpty() }
    }

    private fun checkLimits(files: List<ZipEntryRecord>, root: String, archiveSize: Long, collector: IssueCollector): Boolean {
        files.forEach { entry ->
            val relative = entry.name.removePrefix(root)
            when {
                !ThemePaths.isPlainRelative(relative) ->
                    collector.error(IssueCode.ARCHIVE_UNSAFE_PATH, "«$relative»: недопустимый путь или символы в имени")
                relative.length > limits.maxPathLength ->
                    collector.error(IssueCode.ARCHIVE_LIMIT, "«$relative»: путь длиннее ${limits.maxPathLength} символов")
                relative.count { it == '/' } > limits.maxDepth ->
                    collector.error(IssueCode.ARCHIVE_LIMIT, "«$relative»: вложенность папок больше ${limits.maxDepth}")
                entry.uncompressedSize > limits.maxFileBytes ->
                    collector.error(IssueCode.ARCHIVE_LIMIT, "«$relative»: файл больше ${limits.maxFileBytes / ArchiveLimits.MEGABYTE} МБ")
            }
        }
        if (files.sumOf { it.compressedSize } > archiveSize) {
            collector.error(IssueCode.ARCHIVE_UNREADABLE, "Записи архива ссылаются на одни и те же данные: архив повреждён")
        }
        if (files.sumOf { it.uncompressedSize } > limits.maxTotalBytes) {
            collector.error(IssueCode.ARCHIVE_LIMIT, "Тема после распаковки больше ${limits.maxTotalBytes / ArchiveLimits.MEGABYTE} МБ")
        }
        return collector.issues.isEmpty()
    }

    private fun copy(
        fileSystem: FileSystem,
        reader: ZipDirectoryReader,
        handle: FileHandle,
        entry: ZipEntryRecord,
        destination: Path,
        remainingTotal: Long,
    ): Long {
        val raw = LimitedSource(handle.source(reader.localDataOffset(entry)), entry.compressedSize)
        val source: Source = if (entry.method == DEFLATED) InflaterSource(raw, Inflater(true)) else raw
        val allowed = minOf(limits.maxFileBytes, remainingTotal)
        val written = source.use { input ->
            val output = writing {
                destination.parent?.let(fileSystem::createDirectories)
                fileSystem.sink(destination).buffer()
            }
            try {
                transfer(input, output, minOf(allowed, entry.uncompressedSize), entry)
            } finally {
                writing { output.close() }
            }
        }
        if (written != entry.uncompressedSize) throw ZipFormatException("«${entry.name}» повреждён: размер не совпал")
        return written
    }

    private fun transfer(input: Source, output: BufferedSink, limit: Long, entry: ZipEntryRecord): Long {
        val buffer = Buffer()
        var written = 0L
        var read = input.read(buffer, CHUNK_SIZE)
        while (read != -1L) {
            written += read
            if (written > limit) throw ZipFormatException("«${entry.name}» распаковывается больше заявленного размера")
            writing { output.write(buffer, read) }
            read = input.read(buffer, CHUNK_SIZE)
        }
        return written
    }

    private inline fun <T> writing(block: () -> T): T = try {
        block()
    } catch (error: IOException) {
        throw TargetWriteException(error)
    }

    private companion object {
        const val STORED = 0
        const val DEFLATED = 8
        const val CHUNK_SIZE = 8_192L
    }
}
