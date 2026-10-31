package ru.aokazantsev.devatlas.usage.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class UsageDay(
    val version: String,
    val events: Map<String, Int> = emptyMap(),
    val revision: Int = 1,
    val sentRevision: Int = 0,
)
