package ru.aokazantsev.devatlas.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCase
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents
import ru.aokazantsev.devatlas.navigation.readingStack
import ru.aokazantsev.devatlas.reading.domain.BookmarkRepository
import ru.aokazantsev.devatlas.reading.domain.model.Bookmark
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

@Inject
class BookmarksViewModel(
    observeContents: ObserveContentsUseCase,
    private val bookmarks: BookmarkRepository,
) : ViewModel() {

    private var themeId: String? = null

    private val pendingOrder = MutableStateFlow<List<Long>?>(null)

    private val reorderWrites = Mutex()

    val state: StateFlow<BookmarksState> = combine(observeContents(), pendingOrder, ::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), BookmarksState.Loading)

    fun rename(id: Long, name: String) {
        val item = items().firstOrNull { it.id == id } ?: return
        write { bookmarks.rename(id, BookmarkNames.normalize(name, item.title)) }
    }

    fun delete(id: Long) {
        write { bookmarks.delete(id) }
    }

    fun move(from: Int, to: Int) {
        val theme = themeId ?: return
        val ids = items().map { it.id }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        pendingOrder.value = ids
        viewModelScope.launch {
            try {
                reorderWrites.withLock { bookmarks.reorder(theme, ids) }
            } catch (_: SQLiteException) {
                pendingOrder.compareAndSet(ids, null)
            }
        }
    }

    private fun items(): List<BookmarkItem> = (state.value as? BookmarksState.Ready)?.items.orEmpty()

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (_: SQLiteException) {
            }
        }
    }

    private fun toState(contents: ThemeContents, pending: List<Long>?): BookmarksState {
        if (contents !is ThemeContents.Ready) return BookmarksState.NoTheme
        themeId = contents.theme.id
        val items = contents.bookmarks.mapNotNull { it.toItem(contents.manifest) }
        if (pending == null) return BookmarksState.Ready(items)
        val rank = pending.withIndex().associate { (index, id) -> id to index }
        return BookmarksState.Ready(items.sortedBy { rank[it.id] ?: Int.MAX_VALUE })
    }

    private fun Bookmark.toItem(manifest: ThemeManifest): BookmarkItem? {
        val stack = readingStack(manifest.tree, target, anchor) ?: return null
        val place = when (val shown = target) {
            ScreenTarget.Root -> "Содержание темы"
            is ScreenTarget.Section -> "Раздел «${manifest.tree.titleOf(shown.id).orEmpty()}»"
            is ScreenTarget.Page -> "Страница «${manifest.tree.titleOf(shown.id).orEmpty()}»"
        }
        return BookmarkItem(id = id, title = title, place = place, target = target, stack = stack)
    }

    private fun List<ThemeNode>.titleOf(id: String): String? {
        forEach { node ->
            if (node.id == id) return node.title
            if (node is ThemeNode.Section) node.children.titleOf(id)?.let { return it }
        }
        return null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
