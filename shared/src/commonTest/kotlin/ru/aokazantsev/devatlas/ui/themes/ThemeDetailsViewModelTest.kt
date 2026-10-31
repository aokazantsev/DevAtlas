package ru.aokazantsev.devatlas.ui.themes

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.startup.data.VerifiedManifestsImpl
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.ScriptedInstaller
import ru.aokazantsev.devatlas.themes.ScriptedUpdater
import ru.aokazantsev.devatlas.themes.domain.ThemeLibraryImpl
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository("a")
    private val usage = FakeUsageStatistics()

    private val scopes = ViewModelScopes()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    private suspend fun viewModel(): ThemeDetailsViewModel {
        repository.save(installedOf("a", lastUsedAt = 1L))
        repository.save(installedOf("b", lastUsedAt = 0L))
        return scopes.track(
            ThemeDetailsViewModel(
                themeId = "b",
                library = ThemeLibraryImpl(
                    repository,
                    current,
                    FakeThemeStorage(),
                    FakeProgressRepository(),
                    FakeClock(),
                    FakeJournal(),
                    VerifiedManifestsImpl(),
                ),
                updater = ScriptedUpdater(),
                installer = ScriptedInstaller(),
                usage = usage,
            ),
        )
    }

    @Test
    fun applyMakesThemeCurrent() = runTest(dispatcher) {
        val viewModel = viewModel()
        var applied = false

        viewModel.apply { applied = true }
        advanceUntilIdle()

        assertEquals(true, applied)
        assertEquals("b", current.currentThemeId.first())
        assertEquals(listOf(UsageEvent.THEME_SWITCH), usage.events)
    }

    @Test
    fun databaseFailureOnApplyStaysOnScreenWithError() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)
        repository.databaseFailure = SQLiteException("база занята")
        var applied = false

        viewModel.apply { applied = true }
        advanceUntilIdle()

        assertEquals(false, applied)
        assertNotNull(viewModel.state.value?.applyFailure)
        assertEquals("a", current.currentThemeId.first())
        assertEquals(emptyList(), usage.events)
    }

    @Test
    fun nextApplyClearsPreviousError() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)
        repository.databaseFailure = SQLiteException("база занята")
        viewModel.apply {}
        advanceUntilIdle()
        repository.databaseFailure = null

        viewModel.apply {}
        advanceUntilIdle()

        assertNull(viewModel.state.value?.applyFailure)
    }
}
