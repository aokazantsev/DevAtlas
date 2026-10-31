package ru.aokazantsev.devatlas.usage.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class UsageLedger(
    val days: Map<String, UsageDay> = emptyMap(),
)
