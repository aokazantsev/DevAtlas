package ru.aokazantsev.devatlas.ui.page

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCaseImpl
import ru.aokazantsev.devatlas.contents.FakePageRepository
import ru.aokazantsev.devatlas.contents.domain.model.PageContent
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.page.model.PageDocument
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePackageReader
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.ThemeLibraryImpl
import ru.aokazantsev.devatlas.themes.installedOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PageViewModelTest {

    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository()
    private val storage = FakeThemeStorage()
    private val reader = FakePackageReader()
    private val progress = FakeProgressRepository()
    private val pages = FakePageRepository()
    private val platform = FakePlatformBindings()

    private val content = PageContent(
        document = PageDocument(listOf(PageBlock.Paragraph(listOf(PageInline.Text("Текст"))))),
        diagrams = emptyMap(),
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

    private suspend fun installTheme() {
        repository.save(installedOf("t"))
        current.setCurrent("t")
        val tree = listOf(
            ThemeNode.Page("one", "Первая", "one.html"),
            ThemeNode.Section("s", "Раздел", listOf(ThemeNode.Page("two", "Вторая", "two.html"))),
        )
        reader.manifests[storage.themeDirectory("t")] = ThemeManifest(1, "t", "T", null, tree)
        pages.contents["one.html"] = content
        pages.contents["two.html"] = content
    }

    private fun viewModel(pageId: String): PageViewModel {
        val library = ThemeLibraryImpl(repository, current, storage, progress, FakeClock())
        return scopes.track(
            PageViewModel(pageId, ObserveContentsUseCaseImpl(library, storage, reader, progress), pages, progress, platform),
        )
    }

    private suspend fun PageViewModel.settled() = state.first { it != PageState.Loading }

    @Test
    fun pageShowsContentAndNextPageInTraversalOrder() = runTest {
        installTheme()

        val state = viewModel("one").settled()

        assertEquals(
            PageState.Ready(themeId = "t", title = "Первая", content = content, viewed = false, next = NextPage("two", "Вторая")),
            state,
        )
    }

    @Test
    fun lastPageHasNoNext() = runTest {
        installTheme()

        assertEquals(null, (viewModel("two").settled() as PageState.Ready).next)
    }

    @Test
    fun unknownPageIsMissing() = runTest {
        installTheme()

        assertEquals(PageState.Missing, viewModel("nope").settled())
    }

    @Test
    fun withoutThemePageIsMissing() = runTest {
        assertEquals(PageState.Missing, viewModel("one").settled())
    }

    @Test
    fun unreadableManifestMakesPageUnreadable() = runTest {
        repository.save(installedOf("t"))
        current.setCurrent("t")

        assertEquals(PageState.Unreadable(title = null), viewModel("one").settled())
    }

    @Test
    fun unreadableFileIsReported() = runTest {
        installTheme()
        pages.contents.remove("one.html")

        assertEquals(PageState.Unreadable("Первая"), viewModel("one").settled())
    }

    @Test
    fun viewedMarkIsSetAndClearedManually() = runTest {
        installTheme()
        val viewModel = viewModel("one")
        viewModel.settled()

        viewModel.setViewed(true)
        viewModel.state.first { (it as? PageState.Ready)?.viewed == true }
        assertEquals(setOf("one"), progress.viewed.getValue("t").toSet())

        viewModel.setViewed(false)
        viewModel.state.first { (it as? PageState.Ready)?.viewed == false }
        assertTrue(progress.viewed.getValue("t").isEmpty())
    }

    @Test
    fun documentIsLoadedOncePerThemeVersion() = runTest {
        installTheme()
        val viewModel = viewModel("one")
        viewModel.settled()

        viewModel.setViewed(true)
        viewModel.state.first { (it as? PageState.Ready)?.viewed == true }

        assertEquals(1, pages.loads)
    }

    @Test
    fun newThemeVersionReloadsDocument() = runTest {
        installTheme()
        val viewModel = viewModel("one")
        viewModel.settled()

        val updated = PageContent(document = PageDocument(emptyList()), diagrams = emptyMap())
        pages.contents["one.html"] = updated
        repository.save(installedOf("t", sha256 = "updated"))

        viewModel.state.first { (it as? PageState.Ready)?.content == updated }
        assertEquals(2, pages.loads)
    }

    @Test
    fun copyCodePutsExactTextToClipboard() = runTest {
        installTheme()
        val viewModel = viewModel("one")

        viewModel.copyCode("val a = 1\n    b()")

        assertEquals("val a = 1\n    b()", platform.clipboard)
    }

    @Test
    fun copyCodeReportsPlatformFailure() = runTest {
        installTheme()
        val viewModel = viewModel("one")
        platform.copySucceeds = false

        assertFalse(viewModel.copyCode("x"))
        platform.copySucceeds = true
        assertTrue(viewModel.copyCode("x"))
    }
}
