package ru.aokazantsev.devatlas.ui.bookmarks

import kotlinx.coroutines.Dispatchers
import androidx.sqlite.SQLiteException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCaseImpl
import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.reading.FakeBookmarkRepository
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePackageReader
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.ThemeLibraryImpl
import ru.aokazantsev.devatlas.themes.installedOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {

    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository()
    private val storage = FakeThemeStorage()
    private val reader = FakePackageReader()
    private val progress = FakeProgressRepository()
    private val bookmarks = FakeBookmarkRepository()

    private val scopes = ViewModelScopes()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    private suspend fun installTheme() {
        repository.save(installedOf("t"))
        current.setCurrent("t")
        reader.manifests[storage.themeDirectory("t")] = ThemeManifest(
            1,
            "t",
            "T",
            null,
            listOf(ThemeNode.Section("s", "Раздел", listOf(ThemeNode.Page("p", "Страница", "p.html")))),
        )
    }

    private fun viewModel(): BookmarksViewModel {
        val library = ThemeLibraryImpl(repository, current, storage, progress, FakeClock(), FakeJournal())
        return scopes.track(BookmarksViewModel(ObserveContentsUseCaseImpl(library, storage, reader, progress, bookmarks), bookmarks))
    }

    private suspend fun BookmarksViewModel.items(count: Int): List<BookmarkItem> =
        (state.first { (it as? BookmarksState.Ready)?.items?.size == count } as BookmarksState.Ready).items

    private suspend fun BookmarksViewModel.titles(vararg expected: String) {
        state.first { (it as? BookmarksState.Ready)?.items?.map(BookmarkItem::title) == expected.toList() }
    }

    @Test
    fun itemsCarryPlaceAndTreeStack() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 3, "Моя")
        bookmarks.add("t", ScreenTarget.Section("s"), 0, "Раздел")
        bookmarks.add("t", ScreenTarget.Root, 1, "Корень")

        val items = viewModel().items(3)

        assertEquals(
            listOf(
                BookmarkItem(
                    1,
                    "Моя",
                    "Страница «Страница»",
                    ScreenTarget.Page("p"),
                    listOf(Route.Contents(0), Route.Section("s", 0), Route.Page("p", 3)),
                ),
                BookmarkItem(2, "Раздел", "Раздел «Раздел»", ScreenTarget.Section("s"), listOf(Route.Contents(0), Route.Section("s", 0))),
                BookmarkItem(3, "Корень", "Содержание темы", ScreenTarget.Root, listOf(Route.Contents(1))),
            ),
            items,
        )
    }

    @Test
    fun bookmarkOfVanishedPageIsHidden() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("gone"), 0, "Старая")
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Живая")

        assertEquals(listOf("Живая"), viewModel().items(1).map { it.title })
    }

    @Test
    fun moveReordersAndKeepsHiddenAtEnd() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Первая")
        bookmarks.add("t", ScreenTarget.Page("gone"), 0, "Скрытая")
        bookmarks.add("t", ScreenTarget.Section("s"), 0, "Вторая")
        bookmarks.add("t", ScreenTarget.Root, 0, "Третья")
        val viewModel = viewModel()
        viewModel.items(3)

        viewModel.move(2, 0)

        viewModel.titles("Третья", "Первая", "Вторая")
        assertEquals(listOf("Третья", "Первая", "Вторая", "Скрытая"), bookmarks.of("t").map { it.title })
    }

    private suspend fun installTwo() {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Первая")
        bookmarks.add("t", ScreenTarget.Section("s"), 0, "Вторая")
    }

    @Test
    fun moveShowsNewOrderBeforeStoreAnswersAndRevertsOnFailure() = runTest {
        installTwo()
        val gate = CompletableDeferred<Unit>().also { bookmarks.reorderGates += it }
        val viewModel = viewModel()
        viewModel.items(2)

        viewModel.move(1, 0)
        viewModel.titles("Вторая", "Первая")
        gate.completeExceptionally(SQLiteException("база занята"))

        viewModel.titles("Первая", "Вторая")
        assertEquals(listOf("Первая", "Вторая"), bookmarks.of("t").map { it.title })
    }

    @Test
    fun failureOfEarlierMoveKeepsLaterPendingOrder() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Первая")
        bookmarks.add("t", ScreenTarget.Section("s"), 0, "Вторая")
        bookmarks.add("t", ScreenTarget.Root, 0, "Третья")
        val first = CompletableDeferred<Unit>().also { bookmarks.reorderGates += it }
        val second = CompletableDeferred<Unit>().also { bookmarks.reorderGates += it }
        val viewModel = viewModel()
        viewModel.items(3)

        viewModel.move(2, 0)
        viewModel.titles("Третья", "Первая", "Вторая")
        viewModel.move(2, 1)
        viewModel.titles("Третья", "Вторая", "Первая")
        first.completeExceptionally(SQLiteException("база занята"))

        viewModel.titles("Третья", "Вторая", "Первая")
        second.complete(Unit)
        viewModel.titles("Третья", "Вторая", "Первая")
        assertEquals(listOf("Третья", "Вторая", "Первая"), bookmarks.of("t").map { it.title })
    }

    @Test
    fun failureOfLaterMoveShowsStoredOrderOfEarlier() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Первая")
        bookmarks.add("t", ScreenTarget.Section("s"), 0, "Вторая")
        bookmarks.add("t", ScreenTarget.Root, 0, "Третья")
        val second = CompletableDeferred<Unit>()
        bookmarks.reorderGates += CompletableDeferred<Unit>().also { it.complete(Unit) }
        bookmarks.reorderGates += second
        val viewModel = viewModel()
        viewModel.items(3)

        viewModel.move(2, 0)
        viewModel.move(2, 1)
        second.completeExceptionally(SQLiteException("база занята"))

        viewModel.titles("Третья", "Первая", "Вторая")
        assertEquals(listOf("Третья", "Первая", "Вторая"), bookmarks.of("t").map { it.title })
    }

    @Test
    fun moveOutOfRangeIsIgnored() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Первая")
        val viewModel = viewModel()
        viewModel.items(1)

        viewModel.move(0, 5)

        assertEquals(listOf("Первая"), bookmarks.of("t").map { it.title })
    }

    @Test
    fun renameTrimsAndBlankKeepsOldName() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Старое")
        val viewModel = viewModel()
        viewModel.items(1)

        viewModel.rename(1, "   ")
        assertEquals("Старое", bookmarks.of("t").single().title)

        viewModel.rename(1, "  Новое ")
        viewModel.titles("Новое")
    }

    @Test
    fun deleteRemovesBookmark() = runTest {
        installTheme()
        bookmarks.add("t", ScreenTarget.Page("p"), 0, "Первая")
        val viewModel = viewModel()
        viewModel.items(1)

        viewModel.delete(1)

        viewModel.items(0)
        assertEquals(emptyList(), bookmarks.of("t"))
    }

    @Test
    fun withoutThemeNothingIsShown() = runTest {
        assertEquals(BookmarksState.NoTheme, viewModel().state.first { it != BookmarksState.Loading })
    }
}
