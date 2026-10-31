package ru.aokazantsev.devatlas.update.data

import ru.aokazantsev.devatlas.update.domain.model.AppVersion

internal object UpdateFiles {
    const val APK_FOLDER = "updates"
    const val CACHE_FOLDER = "update-cache"
    const val APK_EXTENSION = ".apk"
    private const val APK_PREFIX = "DevAtlas-"

    fun apkName(version: AppVersion): String = "$APK_PREFIX$version$APK_EXTENSION"

    fun versionOf(apkName: String): AppVersion? =
        if (apkName.startsWith(APK_PREFIX) && apkName.endsWith(APK_EXTENSION)) {
            AppVersion.parse(apkName.removePrefix(APK_PREFIX).removeSuffix(APK_EXTENSION))
        } else {
            null
        }
}
