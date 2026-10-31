package ru.aokazantsev.devatlas.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.IOException
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class OnboardingRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override val consent: Flow<Consent> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            Consent(
                policyVersion = preferences[POLICY_VERSION_KEY] ?: Consent.Default.policyVersion,
                crashReports = preferences[CRASH_REPORTS_KEY] ?: Consent.Default.crashReports,
                usageStatistics = preferences[USAGE_STATISTICS_KEY] ?: Consent.Default.usageStatistics,
            )
        }

    override suspend fun isPolicyAccepted(): Boolean = consent.first().policyAccepted

    override suspend fun accept(crashReports: Boolean, usageStatistics: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[POLICY_VERSION_KEY] = Consent.CURRENT_POLICY_VERSION
                preferences[CRASH_REPORTS_KEY] = crashReports
                preferences[USAGE_STATISTICS_KEY] = usageStatistics
            }
        } catch (_: IOException) {
        }
    }

    override suspend fun setCrashReports(enabled: Boolean) {
        try {
            dataStore.edit { it[CRASH_REPORTS_KEY] = enabled }
        } catch (_: IOException) {
        }
    }

    override suspend fun setUsageStatistics(enabled: Boolean) {
        try {
            dataStore.edit { it[USAGE_STATISTICS_KEY] = enabled }
        } catch (_: IOException) {
        }
    }

    private companion object {
        val POLICY_VERSION_KEY = intPreferencesKey("policy_version")
        val CRASH_REPORTS_KEY = booleanPreferencesKey("crash_reports")
        val USAGE_STATISTICS_KEY = booleanPreferencesKey("usage_statistics")
    }
}
