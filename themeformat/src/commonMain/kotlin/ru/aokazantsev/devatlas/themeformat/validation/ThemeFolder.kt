package ru.aokazantsev.devatlas.themeformat.validation

import okio.FileSystem
import okio.Path

object ThemeFolder {

    private const val MAC_RESOURCE_FOLDER = "__MACOSX"

    fun files(fileSystem: FileSystem, root: Path): Set<String> =
        fileSystem.listRecursively(root)
            .filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }
            .map { it.relativeTo(root).segments.joinToString("/") }
            .filterNot { relative -> relative.split('/').any { it.startsWith(".") || it == MAC_RESOURCE_FOLDER } }
            .toSortedSet()
}
