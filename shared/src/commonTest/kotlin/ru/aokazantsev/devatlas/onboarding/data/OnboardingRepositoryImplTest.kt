package ru.aokazantsev.devatlas.onboarding.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.settings.data.FailingPreferencesDataStore
import ru.aokazantsev.devatlas.settings.data.InMemoryPreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    @Test
    fun acceptedConsentIsReadBack() = runTest {
        val repository = OnboardingRepositoryImpl(InMemoryPreferencesDataStore())

        repository.accept(crashReports = true, usageStatistics = false)

        assertTrue(repository.isPolicyAccepted())
        assertEquals(Consent(Consent.CURRENT_POLICY_VERSION, crashReports = true, usageStatistics = false), repository.consent.first())
    }

    @Test
    fun readIoErrorGivesDefaultConsent() = runTest {
        val repository = OnboardingRepositoryImpl(FailingPreferencesDataStore(IOException("disk")))

        assertEquals(Consent.Default, repository.consent.first())
        assertFalse(repository.isPolicyAccepted())
    }

    @Test
    fun writeIoErrorIsSwallowed() = runTest {
        OnboardingRepositoryImpl(FailingPreferencesDataStore(IOException("disk"))).accept(crashReports = false, usageStatistics = true)
    }
}
