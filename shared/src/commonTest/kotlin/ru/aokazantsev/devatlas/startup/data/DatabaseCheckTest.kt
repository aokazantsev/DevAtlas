package ru.aokazantsev.devatlas.startup.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatabaseCheckTest {

    @Test
    fun passesWhenCountSucceeds() = runTest {
        assertEquals(StartupCheckResult.Passed, DatabaseCheck(dao { 0 }).check())
    }

    @Test
    fun illegalStateBecomesFailed() = runTest {
        val check = DatabaseCheck(dao { throw IllegalStateException("Room cannot verify the data integrity") })

        assertEquals(StartupCheckResult.Failed("Room cannot verify the data integrity"), check.check())
    }

    @Test
    fun illegalStateWithoutMessageUsesTypeName() = runTest {
        assertEquals(StartupCheckResult.Failed("IllegalStateException"), DatabaseCheck(dao { throw IllegalStateException() }).check())
    }

    @Test
    fun otherErrorPropagates() = runTest {
        assertFailsWith<UnsupportedOperationException> { DatabaseCheck(dao { throw UnsupportedOperationException() }).check() }
    }

    @Test
    fun cancellationPropagates() = runTest {
        assertFailsWith<CancellationException> { DatabaseCheck(dao { throw CancellationException("cancelled") }).check() }
    }

    private fun dao(count: () -> Int) = object : ViewedPageDao {
        override suspend fun count(): Int = count()
    }
}
