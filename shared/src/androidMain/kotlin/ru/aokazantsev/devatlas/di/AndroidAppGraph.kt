package ru.aokazantsev.devatlas.di

import android.content.Context
import dev.zacsweers.metro.createGraphFactory

fun createAndroidAppGraph(context: Context, appInfo: AppInfo): AppGraph =
    createGraphFactory<AppGraph.Factory>().create(appInfo = appInfo, platform = AndroidPlatformBindings(context))
