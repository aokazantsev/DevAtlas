package ru.aokazantsev.devatlas.startup.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity
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
        override suspend fun upsert(entity: ViewedPageEntity) = Unit

        override fun observePageIds(themeId: String): Flow<List<String>> = emptyFlow()

        override suspend fun delete(themeId: String, pageId: String) = Unit

        override suspend fun count(): Int = count()

        override suspend fun countForTheme(themeId: String): Int = 0

        override suspend fun deleteForTheme(themeId: String) = Unit

        override suspend fun deleteExcept(themeId: String, keepPageIds: List<String>) = Unit
    }
}
