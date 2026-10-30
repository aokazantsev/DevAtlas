package ru.aokazantsev.devatlas.di

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase

interface PlatformBindings {
    val dataDirectory: Path

    val fileSystem: FileSystem

    fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase>

    fun httpClient(): HttpClient

    fun clipboardText(): String?
}
