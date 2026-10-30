package ru.aokazantsev.devatlas.themecheck

import okio.FileSystem
import okio.Path
import okio.buffer
import ru.aokazantsev.devatlas.themeformat.validation.ThemeFolder
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ThemePacker(private val fileSystem: FileSystem) {

    fun pack(folder: Path, archive: Path): Int {
        val files = ThemeFolder.files(fileSystem, folder).sorted()
        archive.parent?.let(fileSystem::createDirectories)
        fileSystem.sink(archive).use { sink ->
            ZipOutputStream(sink.buffer().outputStream()).use { zip ->
                files.forEach { file -> zip.add(file, fileSystem.read(folder / file) { readByteArray() }) }
            }
        }
        return files.size
    }

    private fun ZipOutputStream.add(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name).apply { timeLocal = FIXED_TIME })
        write(bytes)
        closeEntry()
    }

    private companion object {
        val FIXED_TIME: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)
    }
}
