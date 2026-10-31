package ru.aokazantsev.devatlas.testing

import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

class FakeUsageStatistics : UsageStatistics {
    val events = mutableListOf<UsageEvent>()
    var forgotten = 0
    var sent = 0

    override fun hit(event: UsageEvent) {
        events += event
    }

    override fun forget() {
        forgotten++
    }

    override suspend fun send() {
        sent++
    }
}
