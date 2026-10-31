package ru.aokazantsev.devatlas.crash.data

import kotlinx.coroutines.awaitCancellation
import ru.aokazantsev.devatlas.crash.domain.CrashReports
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CrashCatcherTest {

    private val original = Thread.getDefaultUncaughtExceptionHandler()
    private val recorded = mutableListOf<Pair<String, Throwable>>()
    private val passedOn = mutableListOf<Throwable>()
    private val previous = Thread.UncaughtExceptionHandler { _, error -> passedOn += error }
    private val reports = object : CrashReports {
        override fun record(source: String, error: Throwable) {
            recorded += source to error
        }

        override suspend fun sendPending() = Unit

        override suspend fun awaitSending() {
            awaited++
        }
    }
    private var awaited = 0

    @AfterTest
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(original)
    }

    @Test
    fun crashIsRecordedAndPassedToPreviousHandler() {
        val error = IllegalStateException("boom")

        CrashCatcher({ reports }, previous).uncaughtException(Thread("worker"), error)

        assertEquals(listOf<Pair<String, Throwable>>("thread worker" to error), recorded)
        assertEquals(1, awaited)
        assertEquals(listOf<Throwable>(error), passedOn)
    }

    @Test
    fun failingReportStillReachesPreviousHandler() {
        val crash = IllegalStateException("boom")

        CrashCatcher({ error("graph is broken") }, previous).uncaughtException(Thread("main"), crash)

        assertEquals(listOf<Throwable>(crash), passedOn)
    }

    @Test
    fun hangingSendingDoesNotHoldCrashLongerThanLimit() {
        val hanging = object : CrashReports {
            override fun record(source: String, error: Throwable) = Unit

            override suspend fun sendPending() = Unit

            override suspend fun awaitSending() = awaitCancellation()
        }
        val crash = IllegalStateException("boom")
        val started = System.nanoTime()

        CrashCatcher({ hanging }, previous).uncaughtException(Thread("main"), crash)

        assertEquals(listOf<Throwable>(crash), passedOn)
        assertTrue(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(LIMIT_SECONDS))
    }

    @Test
    fun secondInstallDoesNotWrapItself() {
        Thread.setDefaultUncaughtExceptionHandler(previous)

        installCrashCatcher { reports }
        val installed = Thread.getDefaultUncaughtExceptionHandler()
        installCrashCatcher { reports }

        assertIs<CrashCatcher>(installed)
        assertSame(installed, Thread.getDefaultUncaughtExceptionHandler())
    }

    private companion object {
        const val LIMIT_SECONDS = 5L
    }
}
