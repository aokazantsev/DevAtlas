package ru.aokazantsev.devatlas.startup.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import ru.aokazantsev.devatlas.startup.domain.StartupCheck
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult

@ContributesIntoSet(AppScope::class)
@Inject
internal class InstalledThemesCheck : StartupCheck {

    override suspend fun check(): StartupCheckResult = StartupCheckResult.Passed
}
