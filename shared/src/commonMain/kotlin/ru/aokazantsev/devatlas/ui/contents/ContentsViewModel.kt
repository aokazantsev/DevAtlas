package ru.aokazantsev.devatlas.ui.contents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCase
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents
import ru.aokazantsev.devatlas.reading.domain.BookmarkRepository
import ru.aokazantsev.devatlas.reading.domain.ReadingPointRepository
import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.ui.bookmarks.BookmarkNames

@AssistedInject
class ContentsViewModel(
    @Assisted private val sectionId: String?,
    observeContents: ObserveContentsUseCase,
    private val bookmarks: BookmarkRepository,
    private val readingPoints: ReadingPointRepository,
) : ViewModel() {

    private val target: ScreenTarget = sectionId?.let(ScreenTarget::Section) ?: ScreenTarget.Root

    val state: StateFlow<ContentsState> = observeContents()
        .map(::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ContentsState.Loading)

    fun recordPosition(anchor: Int) {
        val ready = state.value as? ContentsState.Ready ?: return
        readingPoints.record(ReadingPoint(ready.themeId, target, anchor))
    }

    fun addBookmark(name: String, anchor: Int) {
        val ready = state.value as? ContentsState.Ready ?: return
        viewModelScope.launch {
            try {
                bookmarks.add(ready.themeId, target, anchor, BookmarkNames.normalize(name, ready.title))
            } catch (_: SQLiteException) {
            }
        }
    }

    private fun toState(contents: ThemeContents): ContentsState = when (contents) {
        ThemeContents.NoTheme -> ContentsState.NoTheme
        ThemeContents.Unreadable -> ContentsState.Unreadable
        is ThemeContents.Ready -> {
            val viewed = contents.viewedPageIds
            val bookmarked = contents.bookmarks.any { it.target == target }
            if (sectionId == null) {
                ContentsState.Ready(
                    themeId = contents.theme.id,
                    title = contents.theme.title,
                    description = contents.theme.description,
                    progress = progressOf(contents.manifest.tree, viewed),
                    rows = contents.manifest.tree.map { it.toRow(viewed) },
                    bookmarked = bookmarked,
                )
            } else {
                val section = contents.manifest.tree.findSection(sectionId)
                if (section == null) {
                    ContentsState.SectionMissing
                } else {
                    ContentsState.Ready(
                        themeId = contents.theme.id,
                        title = section.title,
                        description = null,
                        progress = null,
                        rows = section.children.map { it.toRow(viewed) },
                        bookmarked = bookmarked,
                    )
                }
            }
        }
    }

    private fun ThemeNode.toRow(viewed: Set<String>): ContentsRow = when (this) {
        is ThemeNode.Page -> ContentsRow.Page(id = id, title = title, viewed = id in viewed)
        is ThemeNode.Section -> ContentsRow.Section(id = id, title = title, progress = progressOf(children, viewed))
    }

    private fun progressOf(nodes: List<ThemeNode>, viewed: Set<String>): ContentsProgress {
        val pageIds = nodes.flatMap { it.pageIds() }
        return ContentsProgress(viewed = pageIds.count { it in viewed }, total = pageIds.size)
    }

    private fun ThemeNode.pageIds(): List<String> = when (this) {
        is ThemeNode.Page -> listOf(id)
        is ThemeNode.Section -> children.flatMap { it.pageIds() }
    }

    private fun List<ThemeNode>.findSection(id: String): ThemeNode.Section? {
        forEach { node ->
            if (node is ThemeNode.Section) {
                if (node.id == id) return node
                node.children.findSection(id)?.let { return it }
            }
        }
        return null
    }

    @AssistedFactory
    fun interface Factory {
        fun create(sectionId: String?): ContentsViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
