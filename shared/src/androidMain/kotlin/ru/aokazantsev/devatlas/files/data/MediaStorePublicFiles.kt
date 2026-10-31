package ru.aokazantsev.devatlas.files.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import okio.IOException
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.files.domain.model.PendingPublicFile
import java.io.FileNotFoundException

internal class MediaStorePublicFiles(private val resolver: ContentResolver) : PublicFiles {

    override val folderName: String = FOLDER

    override fun create(name: String, mimeType: String): PendingPublicFile {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } catch (error: IllegalArgumentException) {
            throw IOException(error.message, error)
        } catch (error: IllegalStateException) {
            throw IOException(error.message, error)
        } catch (error: SecurityException) {
            throw IOException(error.message, error)
        }
        return MediaStorePendingFile(resolver, uri ?: throw IOException("хранилище телефона недоступно"))
    }

    override fun isReadable(uri: String): Boolean = try {
        resolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    } catch (_: FileNotFoundException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: SecurityException) {
        false
    }

    override fun delete(uri: String): Boolean = try {
        resolver.delete(Uri.parse(uri), null, null) > 0
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private companion object {
        const val FOLDER = "DevAtlas"
    }
}
