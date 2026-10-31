package ru.aokazantsev.devatlas.ui.update

import okio.Path
import ru.aokazantsev.devatlas.update.domain.model.AppRelease

internal data class DownloadedUpdate(val file: Path, val release: AppRelease) {

    fun matches(other: AppRelease): Boolean =
        release.version == other.version && release.apk?.sha256 == other.apk?.sha256
}
