package ru.aokazantsev.devatlas.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.startup.domain.RunStartupChecksUseCase
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import ru.aokazantsev.devatlas.ui.splash.SplashState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsCheckingThenReady() = runTest(dispatcher) {
        val viewModel = AppViewModel(QueuedChecks(StartupCheckResult.Passed), FakeThemeSettings())

        assertEquals(SplashState.Checking, viewModel.startupState.value)
        advanceUntilIdle()
        assertEquals(SplashState.Ready, viewModel.startupState.value)
    }

    @Test
    fun failureExposesReason() = runTest(dispatcher) {
        val viewModel = AppViewModel(QueuedChecks(StartupCheckResult.Failed("broken")), FakeThemeSettings())

        advanceUntilIdle()

        assertEquals(SplashState.Failed("broken"), viewModel.startupState.value)
    }

    @Test
    fun retryAfterFailureReachesReady() = runTest(dispatcher) {
        val checks = QueuedChecks(StartupCheckResult.Failed("broken"), StartupCheckResult.Passed)
        val viewModel = AppViewModel(checks, FakeThemeSettings())
        advanceUntilIdle()

        viewModel.retry()

        assertEquals(SplashState.Checking, viewModel.startupState.value)
        advanceUntilIdle()
        assertEquals(SplashState.Ready, viewModel.startupState.value)
        assertEquals(2, checks.calls)
    }

    @Test
    fun appearanceIsNullUntilRepositoryEmits() = runTest(dispatcher) {
        val viewModel = AppViewModel(QueuedChecks(StartupCheckResult.Passed), FakeThemeSettings(initial = null))

        advanceUntilIdle()

        assertNull(viewModel.appearance.value)
    }

    @Test
    fun appearanceFollowsRepository() = runTest(dispatcher) {
        val settings = FakeThemeSettings(initial = Appearance(ThemeMode.DARK, wallpaperColors = true))
        val viewModel = AppViewModel(QueuedChecks(StartupCheckResult.Passed), settings)
        advanceUntilIdle()
        assertEquals(Appearance(ThemeMode.DARK, wallpaperColors = true), viewModel.appearance.value)

        settings.setThemeMode(ThemeMode.LIGHT)
        settings.setWallpaperColors(false)
        advanceUntilIdle()

        assertEquals(Appearance(ThemeMode.LIGHT, wallpaperColors = false), viewModel.appearance.value)
    }

    private class QueuedChecks(vararg results: StartupCheckResult) : RunStartupChecksUseCase {
        private val queue = ArrayDeque(results.toList())
        var calls = 0

        override suspend fun invoke(): StartupCheckResult {
            calls++
            return queue.removeFirst()
        }
    }

    private class FakeThemeSettings(initial: Appearance? = Appearance.Default) : ThemeSettingsRepository {
        private val state = MutableStateFlow(initial)

        override val appearance = state.filterNotNull()

        override suspend fun setThemeMode(mode: ThemeMode) {
            state.value = state.value?.copy(themeMode = mode)
        }

        override suspend fun setWallpaperColors(enabled: Boolean) {
            state.value = state.value?.copy(wallpaperColors = enabled)
        }
    }
}
