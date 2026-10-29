package ru.aokazantsev.devatlas.startup.domain

import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult

class CountingCheck : StartupCheck {
    var calls = 0

    override suspend fun check(): StartupCheckResult {
        calls++
        return StartupCheckResult.Passed
    }
}
