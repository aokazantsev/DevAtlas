package ru.aokazantsev.devatlas.ui.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCase
import ru.aokazantsev.devatlas.contents.domain.PageRepository
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.reading.domain.BookmarkRepository
import ru.aokazantsev.devatlas.reading.domain.ReadingPointRepository
import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.themes.domain.ProgressRepository
import ru.aokazantsev.devatlas.ui.bookmarks.BookmarkNames

@AssistedInject
class PageViewModel(
    @Assisted private val pageId: String,
    observeContents: ObserveContentsUseCase,
    private val pages: PageRepository,
    private val progress: ProgressRepository,
    private val platform: PlatformBindings,
    private val bookmarks: BookmarkRepository,
    private val readingPoints: ReadingPointRepository,
) : ViewModel() {

    private val target = ScreenTarget.Page(pageId)

    private var loaded: LoadedPage? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PageState> = observeContents()
        .mapLatest(::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PageState.Loading)

    fun setViewed(viewed: Boolean) {
        val ready = state.value as? PageState.Ready ?: return
        viewModelScope.launch {
            try {
                progress.setViewed(ready.themeId, pageId, viewed)
            } catch (_: SQLiteException) {
            }
        }
    }

    fun copyCode(code: String): Boolean = platform.copyToClipboard(code)

    fun recordPosition(anchor: Int) {
        val ready = state.value as? PageState.Ready ?: return
        readingPoints.record(ReadingPoint(ready.themeId, target, anchor))
    }

    fun addBookmark(name: String, anchor: Int) {
        val ready = state.value as? PageState.Ready ?: return
        viewModelScope.launch {
            try {
                bookmarks.add(ready.themeId, target, anchor, BookmarkNames.normalize(name, ready.title))
            } catch (_: SQLiteException) {
            }
        }
    }

    private suspend fun toState(contents: ThemeContents): PageState {
        if (contents == ThemeContents.Unreadable) return PageState.Unreadable(title = null)
        if (contents !is ThemeContents.Ready) return PageState.Missing
        val order = contents.manifest.pages
        val index = order.indexOfFirst { it.id == pageId }
        if (index < 0) return PageState.Missing
        val page = order[index]
        val key = "${contents.theme.id}/${contents.theme.sha256}/${page.file}"
        val content = loaded?.takeIf { it.key == key }?.content
            ?: pages.load(contents.theme.id, page).also { loaded = LoadedPage(key, it) }
            ?: return PageState.Unreadable(page.title)
        return PageState.Ready(
            themeId = contents.theme.id,
            title = page.title,
            content = content,
            viewed = pageId in contents.viewedPageIds,
            bookmarked = contents.bookmarks.any { it.target == target },
            next = order.getOrNull(index + 1)?.let { NextPage(it.id, it.title) },
        )
    }

    @AssistedFactory
    fun interface Factory {
        fun create(pageId: String): PageViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
