package ru.aokazantsev.devatlas.update.domain

import okio.Path

interface AppInstaller {
    fun canInstall(): Boolean

    fun openInstallSettings(): Boolean

    fun install(apk: Path): Boolean
}
