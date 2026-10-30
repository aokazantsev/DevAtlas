package ru.aokazantsev.devatlas.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import ru.aokazantsev.devatlas.app.AppViewModel
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.ui.addtheme.AddThemeViewModel
import ru.aokazantsev.devatlas.ui.contents.ContentsViewModel
import ru.aokazantsev.devatlas.ui.onboarding.OnboardingViewModel
import ru.aokazantsev.devatlas.ui.settings.SettingsViewModel
import ru.aokazantsev.devatlas.ui.themes.ManageThemeViewModel
import ru.aokazantsev.devatlas.ui.themes.ThemeDetailsViewModel
import ru.aokazantsev.devatlas.ui.themes.ThemesViewModel

@DependencyGraph(AppScope::class, bindingContainers = [StorageBindings::class])
interface AppGraph {
    val appInfo: AppInfo
    val endpoints: NetworkEndpoints
    val viewedPageDao: ViewedPageDao
    val appViewModel: () -> AppViewModel
    val settingsViewModel: () -> SettingsViewModel
    val onboardingViewModel: () -> OnboardingViewModel
    val addThemeViewModel: () -> AddThemeViewModel
    val contentsViewModel: () -> ContentsViewModel
    val themesViewModel: () -> ThemesViewModel
    val themeDetailsViewModel: ThemeDetailsViewModel.Factory
    val manageThemeViewModel: ManageThemeViewModel.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides appInfo: AppInfo,
            @Provides endpoints: NetworkEndpoints,
            @Provides platform: PlatformBindings,
        ): AppGraph
    }
}
