package ru.aokazantsev.devatlas.ui.addtheme

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.startup.data.VerifiedManifestsImpl
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.ScriptedInstaller
import ru.aokazantsev.devatlas.themes.domain.InstalledThemeRepository
import ru.aokazantsev.devatlas.themes.domain.ThemeLibraryImpl
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.themes.preparedOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddThemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val installer = ScriptedInstaller()
    private val repository = FakeInstalledThemeRepository()
    private val platform = FakePlatformBindings()
    private val endpoints = NetworkEndpoints.Production

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

    private fun viewModel(
        savedState: SavedStateHandle = SavedStateHandle(),
        repository: InstalledThemeRepository = this.repository,
    ) = scopes.track(
        AddThemeViewModel(
            savedState = savedState,
            installer = installer,
            library = ThemeLibraryImpl(
                this.repository,
                FakeCurrentThemeRepository(),
                FakeThemeStorage(),
                FakeProgressRepository(),
                FakeClock(),
                FakeJournal(),
                VerifiedManifestsImpl(),
            ),
            repository = repository,
            platform = platform,
            endpoints = endpoints,
        ),
    )

    @Test
    fun typedAddressSurvivesProcessDeath() {
        val savedState = SavedStateHandle()
        viewModel(savedState).setAddress("https://example.com/a.zip")

        val restored = viewModel(SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) }))

        assertEquals(AddThemeState.Editing(address = "https://example.com/a.zip", hint = null), restored.state.value)
    }

    @Test
    fun databaseFailureAfterDownloadDiscardsItAndShowsError() = runTest(dispatcher) {
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        repository.findFailure = androidx.sqlite.SQLiteException("база занята")
        val viewModel = viewModel()

        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()

        assertTrue(assertIs<AddThemeState.Failed>(viewModel.state.value).failure.message.contains("базу"))
        assertEquals(listOf(prepared), installer.discarded)
    }

    @Test
    fun loadingAgainAfterDownloadDiscardsAbandonedOne() = runTest(dispatcher) {
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        val gate = CompletableDeferred<Unit>()
        val slowRepository = object : InstalledThemeRepository by repository {
            override suspend fun find(themeId: String): InstalledTheme? {
                gate.await()
                return repository.find(themeId)
            }
        }
        val viewModel = viewModel(repository = slowRepository)

        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()
        installer.prepareResult = PrepareResult.Failed(ThemeFailure("нет сети"))
        viewModel.load("https://example.com/b.zip")
        advanceUntilIdle()

        assertEquals(listOf(prepared), installer.discarded)
        assertIs<AddThemeState.Failed>(viewModel.state.value)
    }

    @Test
    fun pasteTakesClipboardTextTrimmed() {
        platform.clipboard = "  https://example.com/a.zip \n"
        val viewModel = viewModel()

        viewModel.paste()

        assertEquals(AddThemeState.Editing("https://example.com/a.zip", hint = null), viewModel.state.value)
    }

    @Test
    fun pasteFromEmptyClipboardSaysSo() {
        platform.clipboard = null
        val viewModel = viewModel()

        viewModel.paste()

        assertEquals(AddThemeState.Editing("", hint = "В буфере обмена пусто"), viewModel.state.value)
    }

    @Test
    fun successfulLoadShowsPreviewWithoutExistingTheme() = runTest(dispatcher) {
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        val viewModel = viewModel()

        viewModel.load("https://example.com/a.zip")
        assertIs<AddThemeState.Loading>(viewModel.state.value)
        advanceUntilIdle()

        assertEquals(AddThemeState.Preview(prepared, existing = null, confirmReplace = false), viewModel.state.value)
        assertEquals(listOf("https://example.com/a.zip"), installer.prepared)
    }

    @Test
    fun failedLoadKeepsAddressAndFailure() = runTest(dispatcher) {
        val failure = ThemeFailure("битый архив", "https://help")
        installer.prepareResult = PrepareResult.Failed(failure)
        val viewModel = viewModel()

        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()

        assertEquals(AddThemeState.Failed("https://example.com/a.zip", failure), viewModel.state.value)
        viewModel.backToEditing()
        assertEquals(AddThemeState.Editing("https://example.com/a.zip", hint = null), viewModel.state.value)
    }

    @Test
    fun sampleCardLoadsConfiguredSampleAddress() = runTest(dispatcher) {
        installer.prepareResult = PrepareResult.Ready(preparedOf("devatlas-sample", "sha", "p1"))
        val viewModel = viewModel()

        viewModel.startWithSample()
        advanceUntilIdle()

        assertEquals(listOf(endpoints.sampleThemeUrl), installer.prepared)
    }

    @Test
    fun installOfNewThemeCommitsAndEmitsId() = runTest(dispatcher) {
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        val viewModel = viewModel()
        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()

        viewModel.install()
        advanceUntilIdle()

        assertEquals(listOf(prepared), installer.installed)
        assertEquals("a", viewModel.installed.first())
    }

    @Test
    fun installFailureShowsReasonWithRetry() = runTest(dispatcher) {
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        installer.installFailure = ThemeFailure("нет места")
        val viewModel = viewModel()
        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()

        viewModel.install()
        advanceUntilIdle()

        assertEquals(AddThemeState.Failed(prepared.source, ThemeFailure("нет места")), viewModel.state.value)
    }

    @Test
    fun installOverExistingThemeAsksFirstAndCanBeDeclined() = runTest(dispatcher) {
        repository.save(installedOf("a"))
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        val viewModel = viewModel()
        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()

        viewModel.install()
        val asking = assertIs<AddThemeState.Preview>(viewModel.state.value)
        assertTrue(asking.confirmReplace)
        assertEquals(emptyList(), installer.installed)

        viewModel.dismissReplace()
        assertEquals(false, assertIs<AddThemeState.Preview>(viewModel.state.value).confirmReplace)

        viewModel.install()
        viewModel.install()
        advanceUntilIdle()
        assertEquals(listOf(prepared), installer.installed)
    }

    @Test
    fun cancelPreviewDiscardsDownloadAndReturnsToAddress() = runTest(dispatcher) {
        val prepared = preparedOf("a", "sha", "p1")
        installer.prepareResult = PrepareResult.Ready(prepared)
        val viewModel = viewModel()
        viewModel.load("https://example.com/a.zip")
        advanceUntilIdle()

        viewModel.cancelPreview()

        assertEquals(listOf(prepared), installer.discarded)
        assertEquals(AddThemeState.Editing(prepared.source, hint = null), viewModel.state.value)
    }

    @Test
    fun sampleIsOfferedOnlyWhileNotInstalled() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(true, viewModel.sampleAvailable.first())

        repository.save(installedOf("devatlas-sample"))

        assertEquals(false, viewModel.sampleAvailable.first())
    }
}
