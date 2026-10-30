package ru.aokazantsev.devatlas.settings.domain

import kotlinx.coroutines.flow.Flow

interface ReadingSettingsRepository {
    val continueFromLastPoint: Flow<Boolean>

    suspend fun setContinueFromLastPoint(enabled: Boolean)
}
