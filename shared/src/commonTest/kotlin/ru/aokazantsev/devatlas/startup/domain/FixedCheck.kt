package ru.aokazantsev.devatlas.startup.domain

import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult

class FixedCheck(private val result: StartupCheckResult) : StartupCheck {
    override suspend fun check(): StartupCheckResult = result
}
