package ru.aokazantsev.devatlas.themes.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase
import ru.aokazantsev.devatlas.database.data.dao.BookmarkDao
import ru.aokazantsev.devatlas.database.data.dao.ReadingPointDao
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themes.domain.ProgressRepository

@ContributesBinding(AppScope::class)
@Inject
internal class ProgressRepositoryImpl(
    private val database: DevAtlasDatabase,
    private val viewedPages: ViewedPageDao,
    private val bookmarks: BookmarkDao,
    private val readingPoints: ReadingPointDao,
) : ProgressRepository {

    override fun observeViewed(themeId: String): Flow<Set<String>> = viewedPages.observePageIds(themeId).map { it.toSet() }

    override suspend fun setViewed(themeId: String, pageId: String, viewed: Boolean) {
        if (viewed) viewedPages.upsert(ViewedPageEntity(themeId, pageId)) else viewedPages.delete(themeId, pageId)
    }

    override suspend fun viewedCount(themeId: String): Int = viewedPages.countForTheme(themeId)

    override suspend fun bookmarkCount(themeId: String): Int = bookmarks.countForTheme(themeId)

    override suspend fun reset(themeId: String) = inTransaction {
        viewedPages.deleteForTheme(themeId)
        bookmarks.deleteForTheme(themeId)
        readingPoints.deleteForTheme(themeId)
    }

    override suspend fun keepOnly(themeId: String, manifest: ThemeManifest) {
        val pageIds = manifest.pages.map { it.id }
        val sectionIds = manifest.tree.sectionIds()
        inTransaction {
            viewedPages.deleteExcept(themeId, pageIds)
            bookmarks.deleteMissing(themeId, pageIds, sectionIds)
            readingPoints.deleteMissing(themeId, pageIds, sectionIds)
        }
    }

    private suspend fun inTransaction(block: suspend () -> Unit) {
        database.useWriterConnection { connection -> connection.immediateTransaction { block() } }
    }

    private fun List<ThemeNode>.sectionIds(): List<String> = flatMap { node ->
        if (node is ThemeNode.Section) listOf(node.id) + node.children.sectionIds() else emptyList()
    }
}
