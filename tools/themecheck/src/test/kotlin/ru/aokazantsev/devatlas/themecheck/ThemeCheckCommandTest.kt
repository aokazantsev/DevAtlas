package ru.aokazantsev.devatlas.themecheck

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeCheckCommandTest {

    private val repository: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }
    private val sampleTheme = File(repository, "sample-theme")
    private val brokenTheme = File(repository, "themeformat/src/jvmTest/resources/fixtures/broken-theme")
    private val workDir: File = Files.createTempDirectory("themecheck").toFile()

    @AfterTest
    fun cleanUp() {
        workDir.deleteRecursively()
    }

    private fun run(vararg args: String): Pair<Int, String> {
        val output = StringBuilder()
        val code = ThemeCheckCommand(output).run(args.toList())
        return code to output.toString()
    }

    @Test
    fun sampleThemePassesAndIsPacked() {
        val archive = File(workDir, "sample-theme.zip")

        val (code, output) = run(sampleTheme.path, "--zip", archive.path, "--strict")

        assertEquals(ThemeCheckCommand.EXIT_OK, code, output)
        assertTrue("Тема «Пример DevAtlas» (devatlas-sample)" in output)
        val names = ZipFile(archive).use { zip -> zip.entries().toList().map { it.name } }
        assertTrue("manifest.json" in names && names.all { !it.startsWith("/") })
        assertEquals(names.sorted(), names)
    }

    @Test
    fun packedArchiveIsCheckedAgain() {
        val archive = File(workDir, "theme.zip")
        run(sampleTheme.path, "--zip", archive.path)

        val (code, output) = run(archive.path)

        assertEquals(ThemeCheckCommand.EXIT_OK, code, output)
    }

    @Test
    fun packingIsDeterministic() {
        val first = File(workDir, "a.zip")
        val second = File(workDir, "b.zip")
        run(sampleTheme.path, "--zip", first.path)
        run(sampleTheme.path, "--zip", second.path)

        assertTrue(first.readBytes().contentEquals(second.readBytes()))
    }

    @Test
    fun brokenThemeFailsWithHumanMessagesAndNoArchive() {
        val archive = File(workDir, "broken.zip")

        val (code, output) = run(brokenTheme.path, "--zip", archive.path)

        assertEquals(ThemeCheckCommand.EXIT_ERRORS, code)
        assertTrue("ошибка  pages/bad.html:2:" in output, output)
        assertTrue("Как исправить: https://aokazantsev.ru/pets/devatlas/themes/#links" in output)
        assertTrue("Архив не собран" in output)
        assertTrue(!archive.exists())
    }

    @Test
    fun wrongArgumentsShowUsage() {
        assertEquals(ThemeCheckCommand.EXIT_USAGE, run().first)
        assertEquals(ThemeCheckCommand.EXIT_USAGE, run("a", "b").first)
        assertEquals(ThemeCheckCommand.EXIT_USAGE, run(File(workDir, "none").path).first)
    }

    @Test
    fun strictTurnsWarningsIntoErrors() {
        val theme = File(workDir, "warn-theme").apply { mkdirs() }
        File(theme, "manifest.json").writeText(
            """{ "formatVersion": 1, "id": "warn", "title": "W", "tree": [ { "id": "a", "title": "A", "file": "a.html" } ] }""",
        )
        File(theme, "a.html").writeText("<p>a</p><img src=\"d.svg\">")
        File(theme, "d.svg").writeText("<svg viewBox=\"0 0 1 1\" fill=\"currentColor\"/>")
        val archive = File(workDir, "warn.zip")

        assertEquals(ThemeCheckCommand.EXIT_OK, run(theme.path).first)
        val (code, output) = run(theme.path, "--strict", "--zip", archive.path)

        assertEquals(ThemeCheckCommand.EXIT_ERRORS, code)
        assertTrue("Архив не собран" in output)
        assertTrue(!archive.exists())
    }

    @Test
    fun unsafeOrBrokenArchiveFails() {
        val unsafe = File(workDir, "unsafe.zip")
        ZipOutputStream(unsafe.outputStream()).use { zip ->
            listOf("manifest.json", "../evil.txt").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
        }
        val broken = File(workDir, "broken.zip")
        run(sampleTheme.path, "--zip", broken.path)
        ZipOutputStream(File(workDir, "broken-theme.zip").outputStream()).use { zip ->
            brokenTheme.walkTopDown().filter { it.isFile }.forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativeTo(brokenTheme).invariantSeparatorsPath))
                zip.write(file.readBytes())
                zip.closeEntry()
            }
        }

        val (unsafeCode, unsafeOutput) = run(unsafe.path)
        assertEquals(ThemeCheckCommand.EXIT_ERRORS, unsafeCode)
        assertTrue("../evil.txt" in unsafeOutput, unsafeOutput)
        assertEquals(ThemeCheckCommand.EXIT_ERRORS, run(File(workDir, "broken-theme.zip").path).first)
        assertEquals(ThemeCheckCommand.EXIT_USAGE, run(broken.path, "--zip", File(workDir, "x.zip").path).first)
        assertTrue(!File(workDir, "evil.txt").exists())
    }
}
