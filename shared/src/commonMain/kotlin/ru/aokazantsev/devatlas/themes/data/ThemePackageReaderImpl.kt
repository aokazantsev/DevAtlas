package ru.aokazantsev.devatlas.themes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.Buffer
import okio.FileSystem
import okio.IOException
import okio.Path
import ru.aokazantsev.devatlas.themeformat.manifest.ManifestParser
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.validation.ThemeFolder
import ru.aokazantsev.devatlas.themes.domain.ThemePackageReader

@ContributesBinding(AppScope::class)
@Inject
internal class ThemePackageReaderImpl(
    private val fileSystem: FileSystem,
) : ThemePackageReader {

    override fun manifest(themeDirectory: Path): ThemeManifest? {
        val file = themeDirectory / ThemeManifest.FILE_NAME
        return try {
            if (!fileSystem.exists(file)) return null
            ManifestParser.parse(fileSystem.read(file) { readUtf8() }.removePrefix(BYTE_ORDER_MARK)).manifest
        } catch (_: IOException) {
            null
        }
    }

    override fun pageHashes(themeDirectory: Path, manifest: ThemeManifest): Map<String, String> =
        manifest.pages.associate { page -> page.id to hashOf(themeDirectory / page.file) }

    override fun contentDigest(themeDirectory: Path): String {
        val listing = Buffer()
        ThemeFolder.files(fileSystem, themeDirectory).forEach { file ->
            listing.writeUtf8(file).writeByte(SEPARATOR).writeUtf8(hashOf(themeDirectory / file)).writeByte(LINE_END)
        }
        return listing.sha256().hex()
    }

    private fun hashOf(file: Path): String = try {
        fileSystem.read(file) { readByteString() }.sha256().hex()
    } catch (_: IOException) {
        ""
    }

    private companion object {
        const val BYTE_ORDER_MARK = "﻿"
        const val SEPARATOR = 0
        const val LINE_END = 10
    }
}
