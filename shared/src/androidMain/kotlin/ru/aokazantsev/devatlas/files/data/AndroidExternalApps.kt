package ru.aokazantsev.devatlas.files.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import ru.aokazantsev.devatlas.files.domain.ExternalApps

internal class AndroidExternalApps(private val context: Context) : ExternalApps {

    override fun openUri(uri: String): Boolean =
        uri.length <= MAX_URI_LENGTH && start(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addCategory(Intent.CATEGORY_BROWSABLE))

    override fun openFile(uri: String, mimeType: String): Boolean = start(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(uri), mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    override fun shareFile(uri: String, mimeType: String): Boolean {
        val parsed = Uri.parse(uri)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, parsed)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, parsed)
        return start(Intent.createChooser(send, null))
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private companion object {
        const val MAX_URI_LENGTH = 64 * 1024
    }
}
