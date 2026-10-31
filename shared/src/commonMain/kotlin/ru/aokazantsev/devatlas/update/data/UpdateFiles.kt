package ru.aokazantsev.devatlas.update.data

import ru.aokazantsev.devatlas.update.domain.model.AppVersion

internal object UpdateFiles {
    const val APK_FOLDER = "updates"
    const val CACHE_FOLDER = "update-cache"
    const val APK_EXTENSION = ".apk"
    private const val APK_PREFIX = "DevAtlas-"

    fun apkName(version: AppVersion): String = "$APK_PREFIX$version$APK_EXTENSION"
}
