package ru.aokazantsev.devatlas.startup.domain

import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import kotlin.test.Test
import kotlin.test.assertEquals

class RunStartupChecksUseCaseImplTest {

    @Test
    fun passesWhenAllChecksPass() = runTest {
        val useCase = RunStartupChecksUseCaseImpl(setOf(FixedCheck(StartupCheckResult.Passed), FixedCheck(StartupCheckResult.Passed)))

        assertEquals(StartupCheckResult.Passed, useCase())
    }

    @Test
    fun returnsFirstFailure() = runTest {
        val failure = StartupCheckResult.Failed("broken")
        val useCase = RunStartupChecksUseCaseImpl(setOf(FixedCheck(StartupCheckResult.Passed), FixedCheck(failure)))

        assertEquals(failure, useCase())
    }

    @Test
    fun stopsAtFirstFailure() = runTest {
        val skipped = CountingCheck()
        val useCase = RunStartupChecksUseCaseImpl(linkedSetOf(FixedCheck(StartupCheckResult.Failed("first")), skipped))

        assertEquals(StartupCheckResult.Failed("first"), useCase())
        assertEquals(0, skipped.calls)
    }

    @Test
    fun passesWithoutChecks() = runTest {
        assertEquals(StartupCheckResult.Passed, RunStartupChecksUseCaseImpl(emptySet())())
    }
}
