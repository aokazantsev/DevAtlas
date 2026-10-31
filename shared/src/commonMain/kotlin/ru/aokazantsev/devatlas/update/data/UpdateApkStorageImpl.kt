package ru.aokazantsev.devatlas.update.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.FileSystem
import okio.IOException
import okio.Path
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.update.domain.UpdateApkStorage
import ru.aokazantsev.devatlas.update.domain.model.AppVersion

@ContributesBinding(AppScope::class)
@Inject
internal class UpdateApkStorageImpl(
    private val fileSystem: FileSystem,
    private val appInfo: AppInfo,
    platform: PlatformBindings,
) : UpdateApkStorage {

    private val folder = platform.dataDirectory / UpdateFiles.APK_FOLDER

    override fun discardAll() {
        delete { true }
    }

    override fun discardInstalled() {
        val installed = AppVersion.parse(appInfo.versionName) ?: return
        delete { apk -> UpdateFiles.versionOf(apk.name)?.let { it <= installed } ?: true }
    }

    private fun delete(selected: (Path) -> Boolean) {
        try {
            if (!fileSystem.exists(folder)) return
            fileSystem.list(folder)
                .filter { it.name.endsWith(UpdateFiles.APK_EXTENSION) && selected(it) }
                .forEach { fileSystem.delete(it, mustExist = false) }
        } catch (_: IOException) {
        }
    }
}
