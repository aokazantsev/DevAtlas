package ru.aokazantsev.devatlas.crash.domain

interface CrashReports {
    fun record(source: String, error: Throwable)

    suspend fun sendPending()

    suspend fun awaitSending()
}
