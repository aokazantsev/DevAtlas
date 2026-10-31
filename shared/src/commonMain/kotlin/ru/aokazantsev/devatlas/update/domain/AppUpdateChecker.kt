package ru.aokazantsev.devatlas.update.domain

import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck

interface AppUpdateChecker {
    suspend fun check(): UpdateCheck

    suspend fun checkIfDue(): UpdateCheck.Available?

    suspend fun markAnnounced(check: UpdateCheck.Available)
}
