package ru.aokazantsev.devatlas.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data class AddTheme(val onboarding: Boolean) : Route

    @Serializable
    data object Contents : Route

    @Serializable
    data class Page(val pageId: String) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Themes : Route

    @Serializable
    data class ThemeDetails(val themeId: String) : Route

    @Serializable
    data class ManageTheme(val themeId: String) : Route

    @Serializable
    data object About : Route

    @Serializable
    data object ComponentCatalog : Route
}
