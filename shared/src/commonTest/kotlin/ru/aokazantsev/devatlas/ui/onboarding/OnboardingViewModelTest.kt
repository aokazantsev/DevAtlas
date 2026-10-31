package ru.aokazantsev.devatlas.ui.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.IOException
import ru.aokazantsev.devatlas.onboarding.data.OnboardingRepositoryImpl
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.settings.data.FailingPreferencesDataStore
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import ru.aokazantsev.devatlas.settings.data.SwitchablePreferencesDataStore
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = OnboardingRepositoryImpl(InMemoryPreferencesDataStore())

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

    @Test
    fun defaultsAreReportsOffAndStatisticsOnAndPolicyNotAccepted() {
        val viewModel = scopes.track(OnboardingViewModel(repository))

        assertEquals(OnboardingState(policyAccepted = false, crashReports = false, usageStatistics = true), viewModel.state.value)
    }

    @Test
    fun nextWithoutPolicyDoesNothing() = runTest(dispatcher) {
        val viewModel = scopes.track(OnboardingViewModel(repository))
        var done = false

        viewModel.next { done = true }
        advanceUntilIdle()

        assertEquals(false, done)
        assertEquals(false, repository.isPolicyAccepted())
    }

    @Test
    fun nextStoresChoicesWithCurrentPolicyVersion() = runTest(dispatcher) {
        val viewModel = scopes.track(OnboardingViewModel(repository))
        var done = false
        viewModel.setPolicyAccepted(true)
        viewModel.setCrashReports(true)
        viewModel.setUsageStatistics(false)

        viewModel.next { done = true }
        advanceUntilIdle()

        assertEquals(true, done)
        assertEquals(Consent(Consent.CURRENT_POLICY_VERSION, crashReports = true, usageStatistics = false), repository.consent.first())
    }

    @Test
    fun failedSaveKeepsOnboardingAndShowsError() = runTest(dispatcher) {
        val viewModel = scopes.track(OnboardingViewModel(OnboardingRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))))
        var done = false
        viewModel.setPolicyAccepted(true)

        viewModel.next { done = true }
        advanceUntilIdle()

        assertEquals(false, done)
        assertNotNull(viewModel.state.value.failure)
    }

    @Test
    fun olderPolicyVersionIsNotAccepted() {
        assertEquals(false, Consent(policyVersion = 0, crashReports = false, usageStatistics = true).policyAccepted)
        assertEquals(true, Consent(Consent.CURRENT_POLICY_VERSION, crashReports = false, usageStatistics = true).policyAccepted)
        assertEquals(false, Consent(Consent.CURRENT_POLICY_VERSION + 1, crashReports = false, usageStatistics = true).policyAccepted)
    }

    @Test
    fun retryAfterFailedSaveSucceeds() = runTest(dispatcher) {
        val store = SwitchablePreferencesDataStore(IOException("disk"))
        val retryRepository = OnboardingRepositoryImpl(store)
        val viewModel = scopes.track(OnboardingViewModel(retryRepository))
        var done = false
        viewModel.setPolicyAccepted(true)
        viewModel.next { done = true }
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.failure)

        store.failure = null
        viewModel.next { done = true }
        advanceUntilIdle()

        assertEquals(true, done)
        assertNull(viewModel.state.value.failure)
        assertEquals(true, retryRepository.isPolicyAccepted())
    }
}
