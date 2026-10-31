package ru.aokazantsev.devatlas.usage.data.model

internal data class UsagePacket(
    val day: String,
    val revision: Int,
    val body: String,
)
