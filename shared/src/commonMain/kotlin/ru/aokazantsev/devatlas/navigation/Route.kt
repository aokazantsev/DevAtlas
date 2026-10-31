package ru.aokazantsev.devatlas.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data class AddTheme(val onboarding: Boolean) : Route

    @Serializable
    data class Contents(val anchor: Int = 0) : Route

    @Serializable
    data object Bookmarks : Route

    @Serializable
    data class Section(val sectionId: String, val anchor: Int = 0) : Route

    @Serializable
    data class Page(val pageId: String, val anchor: Int = 0) : Route

    @Serializable
    data class Web(val url: String) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Downloads : Route

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
