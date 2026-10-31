package ru.aokazantsev.devatlas.ui.themes

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import ru.aokazantsev.devatlas.themes.installedOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ManageThemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository("a")
    private val progress = FakeProgressRepository()
    private val storage = FakeThemeStorage()
    private val updater = ScriptedUpdater()

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

    private suspend fun viewModel(themeId: String = "a", savedState: SavedStateHandle = SavedStateHandle()): ManageThemeViewModel {
        repository.save(installedOf("a", lastUsedAt = 1L))
        repository.save(installedOf("b", lastUsedAt = 0L))
        return scopes.track(
            ManageThemeViewModel(
                themeId = themeId,
                savedState = savedState,
                library = ThemeLibraryImpl(repository, current, storage, progress, FakeClock(), FakeJournal(), VerifiedManifestsImpl()),
                updater = updater,
                installer = ScriptedInstaller(),
                usage = FakeUsageStatistics(),
            ),
        )
    }

    @Test
    fun optionsSurviveProcessDeath() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val first = viewModel(savedState = savedState)
        first.setDeleteTheme(true)
        first.setResetProgress(true)

        val restored = viewModel(savedState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) }))
        advanceUntilIdle()

        assertEquals(true, restored.state.value.deleteTheme)
        assertEquals(true, restored.state.value.resetProgress)
    }

    @Test
    fun databaseFailureWhileCountingProgressIsShown() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setResetProgress(true)
        progress.countFailure = androidx.sqlite.SQLiteException("база занята")

        viewModel.requestRemoval()
        advanceUntilIdle()

        assertNull(viewModel.state.value.confirm)
        assertNotNull(viewModel.state.value.notice)
    }

    @Test
    fun loadsThemeAndItsSourceAddress() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(true, state.loaded)
        assertEquals("https://example.com/a.zip", state.sourceAddress)
        assertEquals(false, state.canContinue)
    }

    @Test
    fun missingThemeIsLoadedAsNull() = runTest(dispatcher) {
        val viewModel = viewModel("missing")
        advanceUntilIdle()

        assertNull(viewModel.state.value.theme)
        assertEquals(true, viewModel.state.value.loaded)
    }

    @Test
    fun savingUnchangedOrBlankSourceDoesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.saveSource()
        viewModel.setSourceAddress("   ")
        viewModel.saveSource()
        advanceUntilIdle()

        assertEquals(emptyList(), updater.requests.toList())
    }

    @Test
    fun savingChangedSourceChecksItTrimmed() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setSourceAddress(" https://example.com/new.zip ")
        viewModel.saveSource()
        advanceUntilIdle()

        assertEquals(listOf("a" to "https://example.com/new.zip"), updater.requests.toList())
    }

    @Test
    fun confirmDescribesWhatWillHappen() = runTest(dispatcher) {
        progress.viewed["a"] = mutableSetOf("p1", "p2", "p3")
        progress.bookmarks["a"] = 2
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setDeleteTheme(true)

        viewModel.requestRemoval()
        advanceUntilIdle()

        val confirm = assertNotNull(viewModel.state.value.confirm)
        assertEquals(
            RemovalConfirm("Тема a", deleteTheme = true, resetProgress = false, viewedCount = 3, bookmarkCount = 2, isCurrent = true),
            confirm,
        )
        viewModel.cancelRemoval()
        assertNull(viewModel.state.value.confirm)
    }

    @Test
    fun requestWithoutOptionsDoesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestRemoval()
        advanceUntilIdle()

        assertNull(viewModel.state.value.confirm)
    }

    @Test
    fun resetOnlyClearsProgressAndOptionsWithoutLeavingScreen() = runTest(dispatcher) {
        progress.viewed["a"] = mutableSetOf("p1")
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setResetProgress(true)
        viewModel.requestRemoval()
        advanceUntilIdle()

        viewModel.confirmRemoval()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Прогресс сброшен", state.notice)
        assertEquals(false, state.resetProgress)
        assertEquals(false, state.busy)
        assertEquals(0, progress.viewedCount("a"))
        assertNotNull(repository.find("a"))
    }

    @Test
    fun deletingCurrentThemeEmitsSwitchToTheOtherOne() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setDeleteTheme(true)
        viewModel.requestRemoval()
        advanceUntilIdle()

        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertEquals(ThemeRemoval.SwitchedTo("b"), viewModel.removals.first())
        assertEquals(listOf("a"), storage.deleted)
    }

    @Test
    fun sourceFollowsSavedThemeUnlessUserEditedIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.save(installedOf("a", lastUsedAt = 1L).copy(sourceUrl = "https://example.com/moved.zip"))
        advanceUntilIdle()
        assertEquals("https://example.com/moved.zip", viewModel.state.value.sourceAddress)

        viewModel.setSourceAddress("https://example.com/typed.zip")
        repository.save(installedOf("a", lastUsedAt = 2L).copy(sourceUrl = "https://example.com/other.zip"))
        advanceUntilIdle()
        assertEquals("https://example.com/typed.zip", viewModel.state.value.sourceAddress)
    }

    @Test
    fun togglingAnOptionClearsNotice() = runTest(dispatcher) {
        progress.viewed["a"] = mutableSetOf("p1")
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.setResetProgress(true)
        viewModel.requestRemoval()
        advanceUntilIdle()
        viewModel.confirmRemoval()
        advanceUntilIdle()

        viewModel.setDeleteTheme(true)

        assertNull(viewModel.state.value.notice)
    }

    @Test
    fun databaseFailureInThemesStreamShowsNotice() = runTest(dispatcher) {
        repository.observeFailure = androidx.sqlite.SQLiteException("база повреждена")

        val viewModel = viewModel()
        advanceUntilIdle()

        val notice = assertNotNull(viewModel.state.value.notice)
        assertEquals(true, notice.startsWith("Не удалось прочитать базу приложения"))
        assertEquals(false, viewModel.state.value.loaded)
    }

    @Test
    fun progressResetClearsSavedOptions() = runTest(dispatcher) {
        progress.viewed["a"] = mutableSetOf("p1")
        val savedState = SavedStateHandle()
        val viewModel = viewModel(savedState = savedState)
        advanceUntilIdle()
        viewModel.setResetProgress(true)
        viewModel.requestRemoval()
        advanceUntilIdle()

        viewModel.confirmRemoval()
        advanceUntilIdle()

        assertEquals(false, savedState.get<Boolean>("resetProgress"))
        assertEquals(false, savedState.get<Boolean>("deleteTheme"))
        val restored = viewModel(savedState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) }))
        advanceUntilIdle()
        assertEquals(false, restored.state.value.resetProgress)
        assertEquals(false, restored.state.value.deleteTheme)
    }
}
