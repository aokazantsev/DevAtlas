package ru.aokazantsev.devatlas.app

import ru.aokazantsev.devatlas.di.AppInfo

internal object AppUserAgent {
    const val APP_NAME = "DevAtlas"

    fun of(appInfo: AppInfo): String = "$APP_NAME/${appInfo.versionName}"
}
