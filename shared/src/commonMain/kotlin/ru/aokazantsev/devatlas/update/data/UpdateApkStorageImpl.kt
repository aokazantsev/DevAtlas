package ru.aokazantsev.devatlas.update.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.FileSystem
import okio.IOException
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.update.domain.UpdateApkStorage

@ContributesBinding(AppScope::class)
@Inject
internal class UpdateApkStorageImpl(
    private val fileSystem: FileSystem,
    platform: PlatformBindings,
) : UpdateApkStorage {

    private val folder = platform.dataDirectory / UpdateFiles.APK_FOLDER

    override fun discardAll() {
        try {
            if (!fileSystem.exists(folder)) return
            fileSystem.list(folder)
                .filter { it.name.endsWith(UpdateFiles.APK_EXTENSION) }
                .forEach { fileSystem.delete(it, mustExist = false) }
        } catch (_: IOException) {
        }
    }
}
