package ru.aokazantsev.devatlas.settings.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemIsDark
    }
}
