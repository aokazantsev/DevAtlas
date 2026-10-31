package ru.aokazantsev.devatlas.files.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import okio.IOException
import okio.Sink
import okio.sink
import ru.aokazantsev.devatlas.files.domain.model.PendingPublicFile
import java.io.FileNotFoundException

internal class MediaStorePendingFile(
    private val resolver: ContentResolver,
    private val contentUri: Uri,
) : PendingPublicFile {

    override val uri: String = contentUri.toString()

    override fun sink(): Sink {
        val stream = try {
            resolver.openOutputStream(contentUri, "w")
        } catch (error: FileNotFoundException) {
            throw IOException(error.message, error)
        } catch (error: SecurityException) {
            throw IOException(error.message, error)
        }
        return stream?.sink() ?: throw IOException("файл не открылся на запись")
    }

    override fun publish(): String? = try {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        if (resolver.update(contentUri, values, null, null) > 0) displayName() else null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: SecurityException) {
        null
    }

    override fun discard() {
        try {
            resolver.delete(contentUri, null, null)
        } catch (_: IllegalArgumentException) {
        } catch (_: SecurityException) {
        }
    }

    private fun displayName(): String? =
        resolver.query(contentUri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
