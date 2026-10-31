package ru.aokazantsev.devatlas.di

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler

interface PlatformBindings {
    val dataDirectory: Path

    val fileSystem: FileSystem

    val publicFiles: PublicFiles

    val externalApps: ExternalApps

    val appInstaller: AppInstaller

    val usageScheduler: UsageScheduler

    val device: DeviceInfo

    fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase>

    fun httpClient(): HttpClient

    fun clipboardText(): String?

    fun copyToClipboard(text: String): Boolean

    fun timeZoneOffsetMillis(epochMillis: Long): Long
}
