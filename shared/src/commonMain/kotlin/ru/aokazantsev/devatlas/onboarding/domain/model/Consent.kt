package ru.aokazantsev.devatlas.onboarding.domain.model

data class Consent(
    val policyVersion: Int,
    val crashReports: Boolean,
    val usageStatistics: Boolean,
) {
    val policyAccepted: Boolean
        get() = policyVersion == CURRENT_POLICY_VERSION

    companion object {
        const val CURRENT_POLICY_VERSION = 1
        const val POLICY_URL = "https://aokazantsev.ru/privacy#devatlas"
        val Default = Consent(policyVersion = 0, crashReports = false, usageStatistics = true)
    }
}
