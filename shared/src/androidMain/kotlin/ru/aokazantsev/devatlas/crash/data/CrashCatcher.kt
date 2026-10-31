package ru.aokazantsev.devatlas.crash.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import ru.aokazantsev.devatlas.crash.domain.CrashReports

internal class CrashCatcher(
    private val reports: () -> CrashReports,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            val crashReports = reports()
            crashReports.record(source = "thread ${thread.name}", error = error)
            runBlocking { withTimeoutOrNull(SENDING_WAIT_MILLIS) { crashReports.awaitSending() } }
        } catch (_: Throwable) {
        }
        previous?.uncaughtException(thread, error)
    }

    private companion object {
        const val SENDING_WAIT_MILLIS = 3_000L
    }
}
