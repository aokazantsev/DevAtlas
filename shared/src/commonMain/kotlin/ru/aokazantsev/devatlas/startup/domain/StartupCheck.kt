package ru.aokazantsev.devatlas.startup.domain

import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult

interface StartupCheck {
    suspend fun check(): StartupCheckResult
}
