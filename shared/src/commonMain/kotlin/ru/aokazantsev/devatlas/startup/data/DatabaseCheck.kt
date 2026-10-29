package ru.aokazantsev.devatlas.startup.data

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.startup.domain.StartupCheck
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult

@ContributesIntoSet(AppScope::class)
@Inject
internal class DatabaseCheck(
    private val viewedPageDao: ViewedPageDao,
) : StartupCheck {

    override suspend fun check(): StartupCheckResult = try {
        viewedPageDao.count()
        StartupCheckResult.Passed
    } catch (error: SQLiteException) {
        StartupCheckResult.Failed(error.message ?: "SQLiteException")
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: IllegalStateException) {
        StartupCheckResult.Failed(error.message ?: "IllegalStateException")
    }
}
