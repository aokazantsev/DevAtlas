package ru.aokazantsev.devatlas.di

import androidx.room.RoomDatabase
import okio.Path
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase

interface PlatformBindings {
    val dataDirectory: Path

    fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase>
}
