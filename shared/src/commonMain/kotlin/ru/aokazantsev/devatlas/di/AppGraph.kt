package ru.aokazantsev.devatlas.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import ru.aokazantsev.devatlas.app.AppViewModel
import ru.aokazantsev.devatlas.ui.settings.SettingsViewModel

@DependencyGraph(AppScope::class, bindingContainers = [StorageBindings::class])
interface AppGraph {
    val appInfo: AppInfo
    val endpoints: NetworkEndpoints
    val appViewModel: () -> AppViewModel
    val settingsViewModel: () -> SettingsViewModel

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides appInfo: AppInfo,
            @Provides endpoints: NetworkEndpoints,
            @Provides platform: PlatformBindings,
        ): AppGraph
    }
}
