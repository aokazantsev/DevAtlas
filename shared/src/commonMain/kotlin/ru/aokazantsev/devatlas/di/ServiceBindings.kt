package ru.aokazantsev.devatlas.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler

@BindingContainer
internal object ServiceBindings {

    @Provides
    fun provideAppInstaller(platform: PlatformBindings): AppInstaller = platform.appInstaller

    @Provides
    fun provideUsageScheduler(platform: PlatformBindings): UsageScheduler = platform.usageScheduler
}
