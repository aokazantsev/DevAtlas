package ru.aokazantsev.devatlas.ui.contents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePackageReader
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.ThemeLibraryImpl
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.themes.manifestOf
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

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ContentsViewModel(
        library = ThemeLibraryImpl(repository, current, storage, FakeProgressRepository(), FakeClock()),
        storage = storage,
        reader = reader,
    )

    @Test
    fun withoutCurrentThemeOffersToAddOne() = runTest {
        assertEquals(ContentsState.NoTheme, viewModel().state.first { it != ContentsState.Loading })
    }

    @Test
    fun currentThemeShowsTitleAndTopLevelNodes() = runTest {
        repository.save(installedOf("a"))
        current.setCurrent("a")
        reader.manifests[storage.themeDirectory("a")] = manifestOf("a", "A", "p1", "p2")

        val state = viewModel().state.first { it != ContentsState.Loading }

        assertEquals(ContentsState.Ready(title = "Тема a", description = null, sections = listOf("p1", "p2")), state)
    }
}
