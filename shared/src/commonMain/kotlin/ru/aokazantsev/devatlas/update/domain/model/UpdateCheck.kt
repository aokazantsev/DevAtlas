package ru.aokazantsev.devatlas.update.domain.model

sealed interface UpdateCheck {
    data class UpToDate(val installed: String) : UpdateCheck

    data class Available(
        val latest: AppRelease,
        val changes: List<AppRelease>,
        val hasOlderChanges: Boolean,
        val offlineNote: String? = null,
    ) : UpdateCheck

    data class Failed(val message: String) : UpdateCheck
}
