package ru.aokazantsev.devatlas.onboarding.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent

interface OnboardingRepository {
    val consent: Flow<Consent>

    suspend fun isPolicyAccepted(): Boolean

    suspend fun accept(crashReports: Boolean, usageStatistics: Boolean): Boolean

    suspend fun setCrashReports(enabled: Boolean)

    suspend fun setUsageStatistics(enabled: Boolean)
}
