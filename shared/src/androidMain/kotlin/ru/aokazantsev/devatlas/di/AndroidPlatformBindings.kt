package ru.aokazantsev.devatlas.di

import android.content.ClipData
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
import ru.aokazantsev.devatlas.files.data.AndroidExternalApps
import ru.aokazantsev.devatlas.files.data.MediaStorePublicFiles
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import java.util.TimeZone

internal class AndroidPlatformBindings(context: Context) : PlatformBindings {

    private val appContext = context.applicationContext

    override val dataDirectory: Path = appContext.filesDir.toOkioPath()

    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override val publicFiles: PublicFiles = MediaStorePublicFiles(appContext.contentResolver)

    override val externalApps: ExternalApps = AndroidExternalApps(appContext)

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

    override fun copyToClipboard(text: String): Boolean {
        val clipboard = appContext.getSystemService(ClipboardManager::class.java)
        return runCatching { clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text)) }.isSuccess
    }

    override fun timeZoneOffsetMillis(epochMillis: Long): Long = TimeZone.getDefault().getOffset(epochMillis).toLong()

    private companion object {
        const val DATABASE_NAME = "devatlas.db"
        const val CLIP_LABEL = "DevAtlas"
        const val CONNECT_TIMEOUT_MILLIS = 15_000L
        const val SOCKET_TIMEOUT_MILLIS = 30_000L
        const val REQUEST_TIMEOUT_MILLIS = 300_000L
    }
}
