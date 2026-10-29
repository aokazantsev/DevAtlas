package ru.aokazantsev.devatlas.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import okio.Path
import okio.Path.Companion.toOkioPath
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase

internal class AndroidPlatformBindings(context: Context) : PlatformBindings {

    private val appContext = context.applicationContext

    override val dataDirectory: Path = appContext.filesDir.toOkioPath()

    override fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase> =
        Room.databaseBuilder<DevAtlasDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
        )

    private companion object {
        const val DATABASE_NAME = "devatlas.db"
    }
}
