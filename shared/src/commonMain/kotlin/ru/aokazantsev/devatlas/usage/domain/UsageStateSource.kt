package ru.aokazantsev.devatlas.usage.domain

interface UsageStateSource {
    suspend fun state(): Map<String, String>
}
