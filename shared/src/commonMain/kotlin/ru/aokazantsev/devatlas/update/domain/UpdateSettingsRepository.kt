package ru.aokazantsev.devatlas.update.domain

import kotlinx.coroutines.flow.Flow

interface UpdateSettingsRepository {
    val autoCheck: Flow<Boolean>

    suspend fun setAutoCheck(enabled: Boolean)

    suspend fun lastAutoCheckMillis(): Long

    suspend fun setLastAutoCheckMillis(millis: Long)

    suspend fun announcedVersion(): String?

    suspend fun setAnnouncedVersion(version: String)
}
