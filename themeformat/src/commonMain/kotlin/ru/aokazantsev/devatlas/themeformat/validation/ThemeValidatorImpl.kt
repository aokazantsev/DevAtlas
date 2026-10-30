package ru.aokazantsev.devatlas.themeformat.validation

import okio.FileSystem
import okio.IOException
import okio.Path
import ru.aokazantsev.devatlas.themeformat.archive.ArchiveLimits
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueCollector
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.manifest.ManifestParser
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themeformat.page.HtmlPageParser
import ru.aokazantsev.devatlas.themeformat.path.ThemePaths
import ru.aokazantsev.devatlas.themeformat.svg.SvgChecker

class ThemeValidatorImpl(
    private val limits: ArchiveLimits = ArchiveLimits.Default,
) : ThemeValidator {

    override fun validate(fileSystem: FileSystem, root: Path): ThemeCheckResult {
        val reader = ThemeFileReader(fileSystem, root, limits)
        val files = try {
            reader.listFiles()
        } catch (error: IOException) {
            val issue = error(null, IssueCode.MANIFEST_MISSING, "Папка темы не читается: ${error.message ?: root.toString()}")
            return ThemeCheckResult(manifest = null, issues = listOf(issue))
        }
        val issues = (checkFolder(files) + checkSizes(files, reader)).toMutableList()
        if (ThemeManifest.FILE_NAME !in files) {
            issues += error(null, IssueCode.MANIFEST_MISSING, "В корне папки темы нет manifest.json")
            return ThemeCheckResult(manifest = null, issues = issues)
        }
        val parsed = reader.readText(ThemeManifest.FILE_NAME, issues)?.let(ManifestParser::parse)
        parsed?.let { issues += it.issues }
        val manifest = parsed?.manifest
        checkContent(manifest, files, reader, issues)
        val sorted = issues.sortedWith(compareBy({ it.file.orEmpty() }, { it.line ?: 0 }, { it.column ?: 0 }))
        val passed = sorted.none { it.severity == IssueSeverity.ERROR }
        return ThemeCheckResult(manifest = manifest.takeIf { passed }, issues = sorted)
    }

    private fun checkContent(manifest: ThemeManifest?, files: Set<String>, reader: ThemeFileReader, issues: MutableList<ThemeIssue>) {
        val usedFiles = mutableSetOf(ThemeManifest.FILE_NAME)
        if (manifest != null) {
            val pageIds = manifest.pages.map { it.id }.toSet()
            manifest.pages.distinctBy { it.file }.forEach { page ->
                if (issues.size < MAX_THEME_ISSUES) checkPage(page, pageIds, files, reader, issues, usedFiles)
            }
        }
        files.filter { ThemePaths.extension(it) == SVG_EXTENSION }.forEach { svg ->
            if (issues.size < MAX_THEME_ISSUES) reader.readText(svg, issues)?.let { issues += SvgChecker.check(svg, it) }
        }
        if (issues.size >= MAX_THEME_ISSUES) {
            issues += error(
                null,
                IssueCode.TOO_MANY_ISSUES,
                "Замечаний больше $MAX_THEME_ISSUES: проверка остановлена, исправьте найденное и запустите снова",
            )
        } else if (manifest != null) {
            issues += unusedFiles(files, usedFiles)
        }
    }

    private fun checkPage(
        page: ThemeNode.Page,
        pageIds: Set<String>,
        files: Set<String>,
        reader: ThemeFileReader,
        issues: MutableList<ThemeIssue>,
        usedFiles: MutableSet<String>,
    ) {
        usedFiles += page.file
        if (page.file !in files) {
            issues += error(
                ThemeManifest.FILE_NAME,
                IssueCode.PAGE_FILE_MISSING,
                "Страница «${page.id}»: файла «${page.file}» нет в папке темы",
            )
            return
        }
        val source = reader.readText(page.file, issues) ?: return
        val parsed = HtmlPageParser.parse(page.file, source)
        issues += parsed.issues
        val collector = IssueCollector(page.file)
        parsed.pageReferences.filter { it.target !in pageIds }.forEach { reference ->
            collector.error(
                IssueCode.LINK_PAGE_MISSING,
                "Ссылка page:${reference.target} ведёт на страницу, которой нет в manifest.json",
                reference.position,
            )
        }
        parsed.imageReferences.forEach { reference ->
            usedFiles += reference.target
            if (reference.target !in files) {
                collector.error(IssueCode.IMAGE_MISSING, "Схемы «${reference.target}» нет в папке темы", reference.position)
            }
        }
        issues += collector.issues
    }

    private fun unusedFiles(files: Set<String>, usedFiles: Set<String>): List<ThemeIssue> =
        files.filter { it !in usedFiles && ThemePaths.extension(it) in CONTENT_EXTENSIONS }.map { unused ->
            ThemeIssue(
                severity = IssueSeverity.WARNING,
                code = IssueCode.FILE_UNUSED,
                message = "Файл не используется: на него нет ссылки ни из manifest.json, ни со страниц",
                file = unused,
            )
        }

    private fun checkFolder(files: Set<String>): List<ThemeIssue> = buildList {
        if (files.size > limits.maxEntries) {
            add(error(null, IssueCode.TOO_MANY_FILES, "В папке темы больше ${limits.maxEntries} файлов"))
        }
        files.filter { ThemePaths.extension(it) !in ALLOWED_EXTENSIONS }.forEach { file ->
            add(error(file, IssueCode.FILE_UNSUPPORTED, "Такие файлы тема не поддерживает: нужны .html, .svg и manifest.json"))
        }
        files.filter { ThemePaths.extension(it) == JSON_EXTENSION && it != ThemeManifest.FILE_NAME }.forEach { file ->
            add(error(file, IssueCode.FILE_UNSUPPORTED, "Из .json файлов тема читает только manifest.json в корне"))
        }
    }

    private fun checkSizes(files: Set<String>, reader: ThemeFileReader): List<ThemeIssue> = buildList {
        val seen = mutableSetOf<String>()
        val sizes = reader.sizes(files)
        files.forEach { file ->
            when {
                !ThemePaths.isPlainRelative(file) ->
                    add(error(file, IssueCode.FILE_UNSUPPORTED, "В имени файла или папки недопустимые символы: \\ / : * ? \" < > |"))
                !seen.add(file.lowercase()) ->
                    add(error(file, IssueCode.FILE_UNSUPPORTED, "Файл с таким же именем, отличающимся только регистром, уже есть"))
                file.length > limits.maxPathLength ->
                    add(error(file, IssueCode.FILE_TOO_LARGE, "Путь длиннее ${limits.maxPathLength} символов"))
                file.count { it == '/' } > limits.maxDepth ->
                    add(error(file, IssueCode.FILE_TOO_LARGE, "Вложенность папок больше ${limits.maxDepth}"))
                sizes.getValue(file) > limits.maxFileBytes ->
                    add(error(file, IssueCode.FILE_TOO_LARGE, "Файл больше ${limits.maxFileBytes / ArchiveLimits.MEGABYTE} МБ"))
            }
        }
        if (sizes.values.sum() > limits.maxTotalBytes) {
            add(error(null, IssueCode.FILE_TOO_LARGE, "Тема больше ${limits.maxTotalBytes / ArchiveLimits.MEGABYTE} МБ"))
        }
    }

    private fun error(file: String?, code: IssueCode, message: String) =
        ThemeIssue(severity = IssueSeverity.ERROR, code = code, message = message, file = file)

    private companion object {
        const val SVG_EXTENSION = "svg"
        const val MAX_THEME_ISSUES = 1000
        const val JSON_EXTENSION = "json"
        val CONTENT_EXTENSIONS = setOf("html", "htm", "svg")
        val ALLOWED_EXTENSIONS = CONTENT_EXTENSIONS + setOf(JSON_EXTENSION, "md", "txt")
    }
}
