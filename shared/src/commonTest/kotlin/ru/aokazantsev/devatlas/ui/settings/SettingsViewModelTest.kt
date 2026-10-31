package ru.aokazantsev.devatlas.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.onboarding.data.OnboardingRepositoryImpl
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import ru.aokazantsev.devatlas.settings.data.ReadingSettingsRepositoryImpl
import ru.aokazantsev.devatlas.settings.data.ThemeSettingsRepositoryImpl
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.update.data.UpdateSettingsRepositoryImpl
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dataStore = InMemoryPreferencesDataStore()
    private val onboarding = OnboardingRepositoryImpl(dataStore)
    private val updateSettings = UpdateSettingsRepositoryImpl(dataStore)
    private val usage = FakeUsageStatistics()
    private val scopes = ViewModelScopes()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    private fun viewModel() = scopes.track(
        SettingsViewModel(
            themeSettings = ThemeSettingsRepositoryImpl(dataStore),
            readingSettings = ReadingSettingsRepositoryImpl(dataStore),
            onboarding = onboarding,
            updateSettings = updateSettings,
            usage = usage,
        ),
    )

    @Test
    fun switchingStatisticsOffForgetsCountersWithoutCountingIt() = runTest {
        onboarding.accept(crashReports = false, usageStatistics = true)
        val viewModel = viewModel()

        viewModel.setUsageStatistics(false)
        advanceUntilIdle()

        assertFalse(onboarding.consent.first().usageStatistics)
        assertEquals(1, usage.forgotten)
        assertTrue(usage.events.isEmpty())
    }

    @Test
    fun otherSwitchesAreStoredAndCounted() = runTest {
        val viewModel = viewModel()
        assertTrue(updateSettings.autoCheck.first())

        viewModel.setCrashReports(true)
        viewModel.setAutoUpdate(false)
        viewModel.setUsageStatistics(true)
        advanceUntilIdle()

        assertTrue(onboarding.consent.first().crashReports)
        assertFalse(updateSettings.autoCheck.first())
        assertEquals(List(3) { UsageEvent.SETTINGS_SAVE }, usage.events)
    }
}
