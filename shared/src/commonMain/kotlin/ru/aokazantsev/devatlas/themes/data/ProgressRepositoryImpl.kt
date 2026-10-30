package ru.aokazantsev.devatlas.themes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.themes.domain.ProgressRepository

@ContributesBinding(AppScope::class)
@Inject
internal class ProgressRepositoryImpl(
    private val dao: ViewedPageDao,
) : ProgressRepository {

    override suspend fun viewedCount(themeId: String): Int = dao.countForTheme(themeId)

    override suspend fun reset(themeId: String) {
        dao.deleteForTheme(themeId)
    }

    override suspend fun keepOnly(themeId: String, pageIds: Collection<String>) {
        dao.deleteExcept(themeId, pageIds.toList())
    }
}
