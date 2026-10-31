package ru.aokazantsev.devatlas.themes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.aokazantsev.devatlas.database.data.dao.InstalledThemeDao
import ru.aokazantsev.devatlas.themes.domain.InstalledThemeRepository
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

@ContributesBinding(AppScope::class)
@Inject
internal class InstalledThemeRepositoryImpl(
    private val dao: InstalledThemeDao,
) : InstalledThemeRepository {

    override fun observeAll(): Flow<List<InstalledTheme>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun all(): List<InstalledTheme> = dao.all().map { it.toDomain() }

    override suspend fun find(themeId: String): InstalledTheme? = dao.find(themeId)?.toDomain()

    override suspend fun save(theme: InstalledTheme) {
        dao.upsert(theme.toEntity())
    }

    override suspend fun delete(themeId: String) {
        dao.delete(themeId)
    }

    override suspend fun touch(themeId: String, now: Long) {
        dao.touch(themeId, now)
    }
}
