package ru.aokazantsev.devatlas.themecheck

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.aokazantsev.devatlas.themeformat.archive.ThemeArchiveExtractorImpl
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.validation.ThemeCheckResult
import ru.aokazantsev.devatlas.themeformat.validation.ThemeValidatorImpl

class ThemeCheckCommand(
    private val output: Appendable,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {

    fun run(args: List<String>): Int {
        val options = ThemeCheckOptions.parse(args) ?: return usage()
        val input = options.input.toPath()
        val isArchive = input.name.lowercase().endsWith(".zip")
        val problem = when {
            !fileSystem.exists(input) -> "Не найдено: $input"
            isArchive && options.zip != null -> "Флаг --zip нужен для папки темы: архив уже собран"
            options.zip != null && isInside(options.zip, options.input) ->
                "Архив --zip нельзя класть внутрь папки темы: он попадёт в саму тему"
            else -> null
        }
        return when {
            problem != null -> {
                output.appendLine(problem)
                EXIT_USAGE
            }
            isArchive -> checkArchive(input, options.strict)
            else -> checkFolder(input, options)
        }
    }

    private fun isInside(archive: String, folder: String): Boolean {
        val archivePath = java.nio.file.Path.of(archive).toAbsolutePath().normalize()
        return archivePath.startsWith(java.nio.file.Path.of(folder).toAbsolutePath().normalize())
    }

    private fun checkFolder(folder: Path, options: ThemeCheckOptions): Int {
        val result = ThemeValidatorImpl().validate(fileSystem, folder)
        report(result.issues, result)
        val code = exitCode(result, options.strict)
        val zip = options.zip
        if (zip != null) {
            if (code == EXIT_OK) {
                val files = ThemePacker(fileSystem).pack(folder, zip.toPath())
                output.appendLine("Собран архив $zip: файлов — $files")
            } else {
                output.appendLine("Архив не собран: сначала исправьте ошибки")
            }
        }
        return code
    }

    private fun checkArchive(archive: Path, strict: Boolean): Int {
        val temporary = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "themecheck-${System.nanoTime()}"
        try {
            val extraction = ThemeArchiveExtractorImpl().extract(fileSystem, archive, temporary)
            if (!extraction.succeeded) {
                report(extraction.issues, null)
                return EXIT_ERRORS
            }
            val result = ThemeValidatorImpl().validate(fileSystem, temporary)
            report(result.issues, result)
            return exitCode(result, strict)
        } finally {
            fileSystem.deleteRecursively(temporary, mustExist = false)
        }
    }

    private fun report(issues: List<ThemeIssue>, result: ThemeCheckResult?) {
        val manifest = result?.manifest
        if (manifest != null) {
            output.appendLine("Тема «${manifest.title}» (${manifest.id}): страниц — ${manifest.pages.size}")
        }
        issues.forEach { issue ->
            val severity = if (issue.severity == IssueSeverity.ERROR) "ошибка" else "предупреждение"
            val location = issue.location.ifEmpty { "тема" }
            output.appendLine("$severity  $location  ${issue.message}")
            output.appendLine("    Как исправить: ${issue.helpUrl}")
        }
        val errors = issues.count { it.severity == IssueSeverity.ERROR }
        val warnings = issues.size - errors
        output.appendLine("Итого: ошибок — $errors, предупреждений — $warnings")
    }

    private fun exitCode(result: ThemeCheckResult, strict: Boolean): Int = when {
        !result.passed -> EXIT_ERRORS
        strict && result.warnings.isNotEmpty() -> EXIT_ERRORS
        else -> EXIT_OK
    }

    private fun usage(): Int {
        output.appendLine(
            """
            Проверка темы DevAtlas.

            themecheck <папка темы | тема.zip> [--zip <архив.zip>] [--strict]

              --zip <архив.zip>  собрать тему в архив, если ошибок нет
              --strict           считать предупреждения ошибками
            """.trimIndent(),
        )
        return EXIT_USAGE
    }

    companion object {
        const val EXIT_OK = 0
        const val EXIT_ERRORS = 1
        const val EXIT_USAGE = 2
    }
}
