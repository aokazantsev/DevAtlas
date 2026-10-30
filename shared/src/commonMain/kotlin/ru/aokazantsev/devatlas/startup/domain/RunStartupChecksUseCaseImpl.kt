package ru.aokazantsev.devatlas.startup.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult

@ContributesBinding(AppScope::class)
@Inject
internal class RunStartupChecksUseCaseImpl(
    private val checks: Set<StartupCheck>,
) : RunStartupChecksUseCase {

    override suspend fun invoke(): StartupCheckResult {
        for (check in checks) {
            val result = check.check()
            if (result != StartupCheckResult.Passed) return result
        }
        return StartupCheckResult.Passed
    }
}
