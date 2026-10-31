package ru.aokazantsev.devatlas

import android.app.Application
import ru.aokazantsev.devatlas.crash.data.installCrashCatcher
import ru.aokazantsev.devatlas.di.AppGraph
import ru.aokazantsev.devatlas.di.AppGraphHolder
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.di.createAndroidAppGraph

open class DevAtlasApplication : Application(), AppGraphHolder {

    override val graph: AppGraph by lazy {
        createAndroidAppGraph(
            context = this,
            appInfo = AppInfo(versionName = BuildConfig.VERSION_NAME, isDebug = BuildConfig.DEBUG),
            endpoints = endpoints(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        installCrashCatcher { graph.crashReports }
    }

    open val holdsSystemSplash: Boolean = true

    protected open fun endpoints(): NetworkEndpoints = NetworkEndpoints.Production
}
