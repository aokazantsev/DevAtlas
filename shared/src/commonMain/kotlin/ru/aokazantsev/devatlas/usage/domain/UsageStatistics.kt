package ru.aokazantsev.devatlas.usage.domain

import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

interface UsageStatistics {
    fun hit(event: UsageEvent)

    fun forget()

    suspend fun send()
}
