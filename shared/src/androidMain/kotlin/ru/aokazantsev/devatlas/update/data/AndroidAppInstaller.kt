package ru.aokazantsev.devatlas.update.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import okio.Path
import ru.aokazantsev.devatlas.update.domain.AppInstaller

internal class AndroidAppInstaller(private val context: Context) : AppInstaller {

    override fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun openInstallSettings(): Boolean =
        start(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))

    override fun install(apk: Path): Boolean {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", apk.toFile())
        } catch (_: IllegalArgumentException) {
            return false
        }
        return start(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
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
        const val AUTHORITY_SUFFIX = ".updates"
        const val APK_TYPE = "application/vnd.android.package-archive"
    }
}
