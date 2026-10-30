package ru.aokazantsev.devatlas.ui.contents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCaseImpl
import ru.aokazantsev.devatlas.reading.FakeBookmarkRepository
import ru.aokazantsev.devatlas.reading.FakeReadingPointRepository
import ru.aokazantsev.devatlas.reading.domain.model.Bookmark
import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
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
class ContentsViewModelTest {

    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository()
    private val storage = FakeThemeStorage()
    private val reader = FakePackageReader()
    private val progress = FakeProgressRepository()
    private val bookmarks = FakeBookmarkRepository()
    private val readingPoints = FakeReadingPointRepository()

    private val tree = listOf(
        page("welcome"),
        ThemeNode.Section(
            id = "basics",
            title = "Основы",
            children = listOf(
                page("intro"),
                ThemeNode.Section(id = "deep", title = "Глубже", children = listOf(page("a"), page("b"))),
            ),
        ),
    )

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

    private fun page(id: String) = ThemeNode.Page(id = id, title = "Стр. $id", file = "$id.html")

    private suspend fun installTheme() {
        repository.save(installedOf("t"))
        current.setCurrent("t")
        reader.manifests[storage.themeDirectory("t")] = ThemeManifest(1, "t", "T", "Описание", tree)
    }

    private fun viewModel(sectionId: String? = null): ContentsViewModel {
        val library = ThemeLibraryImpl(repository, current, storage, progress, FakeClock())
        val contents = ObserveContentsUseCaseImpl(library, storage, reader, progress, bookmarks)
        return scopes.track(ContentsViewModel(sectionId, contents, bookmarks, readingPoints))
    }

    private suspend fun ContentsViewModel.settled() = state.first { it != ContentsState.Loading }

    @Test
    fun withoutCurrentThemeOffersToAddOne() = runTest {
        assertEquals(ContentsState.NoTheme, viewModel().settled())
    }

    @Test
    fun unreadableManifestIsReported() = runTest {
        repository.save(installedOf("t"))
        current.setCurrent("t")

        assertEquals(ContentsState.Unreadable, viewModel().settled())
    }

    @Test
    fun rootShowsTopLevelNodesWithCountersAndOverallProgress() = runTest {
        installTheme()
        progress.viewed["t"] = mutableSetOf("welcome", "a", "gone")

        val state = viewModel().settled()

        assertEquals(
            ContentsState.Ready(
                themeId = "t",
                title = "Тема t",
                description = null,
                progress = ContentsProgress(viewed = 2, total = 4),
                rows = listOf(
                    ContentsRow.Page(id = "welcome", title = "Стр. welcome", viewed = true),
                    ContentsRow.Section(id = "basics", title = "Основы", progress = ContentsProgress(viewed = 1, total = 3)),
                ),
                bookmarked = false,
            ),
            state,
        )
    }

    @Test
    fun nestedSectionShowsItsChildrenWithoutOverallProgress() = runTest {
        installTheme()
        progress.viewed["t"] = mutableSetOf("a", "b")

        val state = viewModel(sectionId = "deep").settled()

        assertEquals(
            ContentsState.Ready(
                themeId = "t",
                title = "Глубже",
                description = null,
                progress = null,
                rows = listOf(
                    ContentsRow.Page(id = "a", title = "Стр. a", viewed = true),
                    ContentsRow.Page(id = "b", title = "Стр. b", viewed = true),
                ),
                bookmarked = false,
            ),
            state,
        )
    }

    @Test
    fun unknownSectionIsReported() = runTest {
        installTheme()

        assertEquals(ContentsState.SectionMissing, viewModel(sectionId = "nope").settled())
    }

    @Test
    fun countersFollowViewedMarks() = runTest {
        installTheme()
        val viewModel = viewModel()
        viewModel.settled()

        progress.setViewed("t", "intro", viewed = true)

        val ready = viewModel.state.first { (it as? ContentsState.Ready)?.progress?.viewed == 1 } as ContentsState.Ready
        assertEquals(ContentsProgress(viewed = 1, total = 3), (ready.rows[1] as ContentsRow.Section).progress)
    }

    @Test
    fun switchingThemeFollowsMarksOfNewTheme() = runTest {
        installTheme()
        repository.save(installedOf("u"))
        reader.manifests[storage.themeDirectory("u")] = ThemeManifest(1, "u", "U", null, listOf(page("x"), page("y")))
        progress.viewed["t"] = mutableSetOf("welcome")
        progress.viewed["u"] = mutableSetOf("x", "y")
        val viewModel = viewModel()
        viewModel.settled()

        current.setCurrent("u")

        val ready = viewModel.state.first { (it as? ContentsState.Ready)?.title == "Тема u" } as ContentsState.Ready
        assertEquals(ContentsProgress(viewed = 2, total = 2), ready.progress)
    }

    @Test
    fun emptyProgressHasZeroFraction() {
        assertEquals(0f, ContentsProgress(viewed = 0, total = 0).fraction)
    }

    @Test
    fun bookmarkOnSectionUsesSectionTitleWhenNameIsBlank() = runTest {
        installTheme()
        val viewModel = viewModel(sectionId = "deep")
        viewModel.settled()

        viewModel.addBookmark("   ", anchor = 1)

        assertEquals(listOf(Bookmark(1, ScreenTarget.Section("deep"), 1, "Глубже")), bookmarks.of("t"))
        assertEquals(true, (viewModel.state.first { (it as? ContentsState.Ready)?.bookmarked == true } as ContentsState.Ready).bookmarked)
    }

    @Test
    fun bookmarkOnRootKeepsTrimmedName() = runTest {
        installTheme()
        val viewModel = viewModel()
        viewModel.settled()

        viewModel.addBookmark("  Начало  ", anchor = 0)

        assertEquals(listOf(Bookmark(1, ScreenTarget.Root, 0, "Начало")), bookmarks.of("t"))
    }

    @Test
    fun positionIsRecordedForCurrentThemeAndLevel() = runTest {
        installTheme()
        val root = viewModel()
        val section = viewModel(sectionId = "basics")
        root.settled()
        section.settled()

        root.recordPosition(1)
        section.recordPosition(0)

        assertEquals(
            listOf(ReadingPoint("t", ScreenTarget.Root, 1), ReadingPoint("t", ScreenTarget.Section("basics"), 0)),
            readingPoints.recorded,
        )
    }

    @Test
    fun positionIsNotRecordedWithoutTheme() = runTest {
        val viewModel = viewModel()
        viewModel.settled()

        viewModel.recordPosition(3)

        assertEquals(emptyList(), readingPoints.recorded)
    }
}
