package ru.aokazantsev.devatlas.di

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase

internal class AndroidPlatformBindings(context: Context) : PlatformBindings {

    private val appContext = context.applicationContext

    override val dataDirectory: Path = appContext.filesDir.toOkioPath()

    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase> =
        Room.databaseBuilder<DevAtlasDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
        )

    override fun httpClient(): HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }

    override fun clipboardText(): String? {
        val clipboard = appContext.getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(appContext)?.toString()
    }

    private companion object {
        const val DATABASE_NAME = "devatlas.db"
        const val CONNECT_TIMEOUT_MILLIS = 15_000L
        const val SOCKET_TIMEOUT_MILLIS = 30_000L
        const val REQUEST_TIMEOUT_MILLIS = 300_000L
    }
}
