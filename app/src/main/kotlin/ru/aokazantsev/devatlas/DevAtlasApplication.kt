package ru.aokazantsev.devatlas

import android.app.Application
import ru.aokazantsev.devatlas.di.AppGraph
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.di.createAndroidAppGraph

open class DevAtlasApplication : Application() {

    val graph: AppGraph by lazy {
        createAndroidAppGraph(
            context = this,
            appInfo = AppInfo(versionName = BuildConfig.VERSION_NAME, isDebug = BuildConfig.DEBUG),
            endpoints = endpoints(),
        )
    }

    open val holdsSystemSplash: Boolean = true

    protected open fun endpoints(): NetworkEndpoints = NetworkEndpoints.Production
}
