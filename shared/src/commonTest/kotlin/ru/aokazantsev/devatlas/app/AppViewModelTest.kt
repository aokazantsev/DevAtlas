package ru.aokazantsev.devatlas.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.reading.FakeReadingPointRepository
import ru.aokazantsev.devatlas.reading.FakeReadingSettings
import ru.aokazantsev.devatlas.reading.domain.FindContinuationUseCaseImpl
import ru.aokazantsev.devatlas.reading.domain.model.ReadingPoint
import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.settings.domain.model.Appearance
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.startup.data.VerifiedManifestsImpl
import ru.aokazantsev.devatlas.startup.domain.RunStartupChecksUseCase
import ru.aokazantsev.devatlas.startup.domain.model.StartupCheckResult
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeLibraryImpl
import ru.aokazantsev.devatlas.themes.domain.model.DamagedTheme
import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.ui.splash.SplashState
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val scopes = ViewModelScopes()

    private val storage = FakeThemeStorage()

    private val verifiedManifests = VerifiedManifestsImpl()
    private val usage = FakeUsageStatistics()

    private val readingPoints = FakeReadingPointRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    @Test
    fun startsCheckingThenReady() = runTest(dispatcher) {
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.Passed))

        assertEquals(SplashState.Checking, viewModel.startupState.value)
        advanceUntilIdle()
        assertEquals(SplashState.Ready, viewModel.startupState.value)
    }

    @Test
    fun failureExposesReason() = runTest(dispatcher) {
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.Failed("broken")))

        advanceUntilIdle()

        assertEquals(SplashState.Failed("broken"), viewModel.startupState.value)
    }

    @Test
    fun retryAfterFailureReachesReady() = runTest(dispatcher) {
        val checks = QueuedChecks(StartupCheckResult.Failed("broken"), StartupCheckResult.Passed)
        val viewModel = viewModelOf(checks)
        advanceUntilIdle()

        viewModel.retry()

        assertEquals(SplashState.Checking, viewModel.startupState.value)
        advanceUntilIdle()
        assertEquals(SplashState.Ready, viewModel.startupState.value)
        assertEquals(2, checks.calls)
    }

    @Test
    fun appearanceIsNullUntilRepositoryEmits() = runTest(dispatcher) {
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.Passed), settings = FakeThemeSettings(initial = null))

        advanceUntilIdle()

        assertNull(viewModel.appearance.value)
    }

    @Test
    fun appearanceFollowsRepository() = runTest(dispatcher) {
        val settings = FakeThemeSettings(initial = Appearance(ThemeMode.DARK, wallpaperColors = true))
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.Passed), settings = settings)
        advanceUntilIdle()
        assertEquals(Appearance(ThemeMode.DARK, wallpaperColors = true), viewModel.appearance.value)

        settings.setThemeMode(ThemeMode.LIGHT)
        settings.setWallpaperColors(false)
        advanceUntilIdle()

        assertEquals(Appearance(ThemeMode.LIGHT, wallpaperColors = false), viewModel.appearance.value)
    }

    @Test
    fun startRouteIsOnboardingUntilPolicyAccepted() = runTest(dispatcher) {
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.Passed))

        advanceUntilIdle()

        assertEquals(listOf(Route.Onboarding), viewModel.startStack.value)
    }

    @Test
    fun startRouteAsksForThemeWhenPolicyAcceptedButNoThemes() = runTest(dispatcher) {
        val onboarding = FakeOnboarding(accepted = true)
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.Passed), onboarding = onboarding)

        advanceUntilIdle()

        assertEquals(listOf(Route.AddTheme(onboarding = true)), viewModel.startStack.value)
    }

    @Test
    fun startRouteIsContentsWhenThemeInstalled() = runTest(dispatcher) {
        val repository = FakeInstalledThemeRepository()
        repository.save(installedOf("a"))
        val viewModel = viewModelOf(
            QueuedChecks(StartupCheckResult.Passed),
            onboarding = FakeOnboarding(accepted = true),
            repository = repository,
        )

        advanceUntilIdle()

        assertEquals(listOf(Route.Contents()), viewModel.startStack.value)
    }

    @Test
    fun continuationRebuildsTreePathToLastPage() = runTest(dispatcher) {
        readingPoints.points["a"] = ReadingPoint("a", ScreenTarget.Page("p2"), anchor = 5)
        val viewModel = continuationViewModel()

        assertEquals(
            listOf(Route.Contents(anchor = 1), Route.Section("s", anchor = 1), Route.Page("p2", anchor = 5)),
            viewModel.startStack.filterNotNull().first(),
        )
    }

    @Test
    fun continuationOpensRootWithItsAnchor() = runTest(dispatcher) {
        readingPoints.points["a"] = ReadingPoint("a", ScreenTarget.Root, anchor = 1)
        val viewModel = continuationViewModel()

        assertEquals(listOf(Route.Contents(anchor = 1)), viewModel.startStack.filterNotNull().first())
    }

    @Test
    fun disabledContinuationOpensContents() = runTest(dispatcher) {
        readingPoints.points["a"] = ReadingPoint("a", ScreenTarget.Page("p2"), anchor = 5)
        val viewModel = continuationViewModel(FakeReadingSettings(enabled = false))

        assertEquals(listOf(Route.Contents()), viewModel.startStack.filterNotNull().first())
    }

    @Test
    fun pointOfVanishedPageFallsBackToContents() = runTest(dispatcher) {
        readingPoints.points["a"] = ReadingPoint("a", ScreenTarget.Page("gone"), anchor = 2)
        val viewModel = continuationViewModel()

        assertEquals(listOf(Route.Contents()), viewModel.startStack.filterNotNull().first())
    }

    @Test
    fun pointOfAnotherThemeIsIgnored() = runTest(dispatcher) {
        readingPoints.points["b"] = ReadingPoint("b", ScreenTarget.Page("p2"), anchor = 2)
        val viewModel = continuationViewModel()

        assertEquals(listOf(Route.Contents()), viewModel.startStack.filterNotNull().first())
    }

    @Test
    fun damagedThemeIsExposedAndRedownloadRerunsChecks() = runTest(dispatcher) {
        val damaged = DamagedTheme("a", "Тема a", "нет файлов")
        val checks = QueuedChecks(StartupCheckResult.ThemeDamaged(damaged), StartupCheckResult.Passed)
        val installer = FakeInstaller(failure = null)
        val viewModel = viewModelOf(checks, installer = installer)
        advanceUntilIdle()
        assertEquals(SplashState.ThemeDamaged(damaged), viewModel.startupState.value)

        viewModel.redownloadDamagedTheme()
        assertEquals("Скачиваю тему заново…", (viewModel.startupState.value as SplashState.ThemeDamaged).busyText)
        advanceUntilIdle()

        assertEquals(listOf("a"), installer.redownloaded)
        assertEquals(SplashState.Ready, viewModel.startupState.value)
    }

    @Test
    fun failedRedownloadKeepsDamagedScreenWithReason() = runTest(dispatcher) {
        val damaged = DamagedTheme("a", "Тема a", "нет файлов")
        val failure = ThemeFailure("нет сети")
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.ThemeDamaged(damaged)), installer = FakeInstaller(failure))
        advanceUntilIdle()

        viewModel.redownloadDamagedTheme()
        advanceUntilIdle()

        assertEquals(SplashState.ThemeDamaged(damaged, busyText = null, failure = failure), viewModel.startupState.value)
    }

    @Test
    fun chooseAnotherThemeForgetsDamagedThemeKeepingProgress() = runTest(dispatcher) {
        val damaged = DamagedTheme("a", "Тема a", "нет файлов")
        val repository = FakeInstalledThemeRepository()
        repository.save(installedOf("a", lastUsedAt = 1L))
        repository.save(installedOf("b", lastUsedAt = 0L))
        val progress = FakeProgressRepository().also { it.viewed["a"] = mutableSetOf("p1") }
        val viewModel = viewModelOf(
            QueuedChecks(StartupCheckResult.ThemeDamaged(damaged), StartupCheckResult.Passed),
            onboarding = FakeOnboarding(accepted = true),
            repository = repository,
            progress = progress,
            current = FakeCurrentThemeRepository("a"),
        )
        advanceUntilIdle()

        viewModel.chooseAnotherTheme()
        assertEquals("Убираю тему…", (viewModel.startupState.value as SplashState.ThemeDamaged).busyText)
        advanceUntilIdle()

        assertNull(repository.find("a"))
        assertEquals(listOf(UsageEvent.THEME_DELETE), usage.events)
        assertEquals(1, progress.viewedCount("a"))
        assertEquals(SplashState.Ready, viewModel.startupState.value)
        assertEquals(listOf(Route.Contents()), viewModel.startStack.value)
    }

    @Test
    fun failedForgetKeepsDamagedScreenWithReason() = runTest(dispatcher) {
        val damaged = DamagedTheme("a", "Тема a", "нет файлов")
        val repository = FakeInstalledThemeRepository()
        repository.save(installedOf("a"))
        repository.databaseFailure = androidx.sqlite.SQLiteException("база занята")
        val viewModel = viewModelOf(QueuedChecks(StartupCheckResult.ThemeDamaged(damaged)), repository = repository)
        advanceUntilIdle()

        viewModel.chooseAnotherTheme()
        advanceUntilIdle()

        val state = viewModel.startupState.value as SplashState.ThemeDamaged
        assertEquals(null, state.busyText)
        assertEquals(true, state.failure?.message?.startsWith("Не удалось изменить базу приложения"))
        assertEquals(emptyList(), usage.events)
    }

    @Suppress("LongParameterList")
    private fun viewModelOf(
        checks: RunStartupChecksUseCase,
        settings: ThemeSettingsRepository = FakeThemeSettings(),
        onboarding: FakeOnboarding = FakeOnboarding(accepted = false),
        repository: FakeInstalledThemeRepository = FakeInstalledThemeRepository(),
        progress: FakeProgressRepository = FakeProgressRepository(),
        current: FakeCurrentThemeRepository = FakeCurrentThemeRepository(),
        installer: ThemeInstaller = FakeInstaller(failure = null),
        readingSettings: FakeReadingSettings = FakeReadingSettings(),
    ): AppViewModel {
        val library = ThemeLibraryImpl(repository, current, storage, progress, FakeClock())
        return scopes.track(
            AppViewModel(
                runStartupChecks = checks,
                onboarding = onboarding,
                library = library,
                installer = installer,
                findContinuation = FindContinuationUseCaseImpl(readingSettings, readingPoints, verifiedManifests),
                usage = usage,
                themeSettings = settings,
            ),
        )
    }

    private suspend fun continuationViewModel(readingSettings: FakeReadingSettings = FakeReadingSettings()): AppViewModel {
        val repository = FakeInstalledThemeRepository()
        repository.save(installedOf("a"))
        val manifest = ThemeManifest(
            formatVersion = 1,
            id = "a",
            title = "A",
            description = null,
            tree = listOf(
                ThemeNode.Page("intro", "Вступление", "intro.html"),
                ThemeNode.Section("s", "Раздел", listOf(ThemeNode.Page("p1", "П1", "p1.html"), ThemeNode.Page("p2", "П2", "p2.html"))),
            ),
        )
        verifiedManifests.replace(mapOf("a" to manifest))
        return viewModelOf(
            QueuedChecks(StartupCheckResult.Passed),
            onboarding = FakeOnboarding(accepted = true),
            repository = repository,
            current = FakeCurrentThemeRepository("a"),
            readingSettings = readingSettings,
        )
    }

    private class FakeOnboarding(accepted: Boolean) : OnboardingRepository {
        override val consent = MutableStateFlow(
            Consent(policyVersion = if (accepted) Consent.CURRENT_POLICY_VERSION else 0, crashReports = false, usageStatistics = true),
        )

        override suspend fun isPolicyAccepted(): Boolean = consent.value.policyAccepted

        override suspend fun accept(crashReports: Boolean, usageStatistics: Boolean) = Unit

        override suspend fun setCrashReports(enabled: Boolean) = Unit

        override suspend fun setUsageStatistics(enabled: Boolean) = Unit
    }

    private class FakeInstaller(private val failure: ThemeFailure?) : ThemeInstaller {
        val redownloaded = mutableListOf<String>()

        override suspend fun prepare(address: String): PrepareResult = error("не используется")

        override suspend fun install(prepared: PreparedTheme): InstallResult = error("не используется")

        override fun discard(prepared: PreparedTheme) = Unit

        override suspend fun redownload(themeId: String): ThemeFailure? {
            redownloaded += themeId
            return failure
        }
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
