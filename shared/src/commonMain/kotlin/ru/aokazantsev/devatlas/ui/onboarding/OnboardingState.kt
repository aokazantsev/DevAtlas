package ru.aokazantsev.devatlas.ui.onboarding

data class OnboardingState(
    val policyAccepted: Boolean,
    val crashReports: Boolean,
    val usageStatistics: Boolean,
    val failure: String? = null,
)
