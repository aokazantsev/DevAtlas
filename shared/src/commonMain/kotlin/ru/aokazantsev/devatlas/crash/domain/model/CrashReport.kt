package ru.aokazantsev.devatlas.crash.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CrashReport(
    val app: String,
    val version: String,
    val os: String,
    val model: String,
    val time: String,
    val source: String,
    val error: String,
    val log: String,
)
