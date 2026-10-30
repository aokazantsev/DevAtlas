package ru.aokazantsev.devatlas.ui.contents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCaseImpl
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

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
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
        return ContentsViewModel(sectionId, ObserveContentsUseCaseImpl(library, storage, reader, progress))
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
                title = "Тема t",
                description = null,
                progress = ContentsProgress(viewed = 2, total = 4),
                rows = listOf(
                    ContentsRow.Page(id = "welcome", title = "Стр. welcome", viewed = true),
                    ContentsRow.Section(id = "basics", title = "Основы", progress = ContentsProgress(viewed = 1, total = 3)),
                ),
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
                title = "Глубже",
                description = null,
                progress = null,
                rows = listOf(
                    ContentsRow.Page(id = "a", title = "Стр. a", viewed = true),
                    ContentsRow.Page(id = "b", title = "Стр. b", viewed = true),
                ),
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
}
