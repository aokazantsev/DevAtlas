package ru.aokazantsev.devatlas.crash.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.IOException
import ru.aokazantsev.devatlas.app.AppUserAgent
import ru.aokazantsev.devatlas.crash.domain.CrashReports
import ru.aokazantsev.devatlas.crash.domain.model.CrashReport
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.upload.domain.ServiceUploader
import ru.aokazantsev.devatlas.upload.domain.model.UploadTarget
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class CrashReportsImpl(
    private val fileSystem: FileSystem,
    private val platform: PlatformBindings,
    private val appInfo: AppInfo,
    private val clock: Clock,
    private val journal: AppJournal,
    private val onboarding: OnboardingRepository,
    private val uploader: ServiceUploader,
) : CrashReports {

    private val folder = platform.dataDirectory / FOLDER
    private val pending = folder / PENDING_FILE
    private val sendings = MutableStateFlow(0)

    override fun record(source: String, error: Throwable) {
        val report = CrashReport(
            app = AppUserAgent.APP_NAME,
            version = appInfo.versionName,
            os = "Android ${platform.device.androidVersion}",
            model = platform.device.model,
            time = clock.now().toString(),
            source = source,
            error = error.stackTraceToString().take(MAX_ERROR_LENGTH),
            log = journal.tail(LOG_TAIL_BYTES),
        )
        try {
            fileSystem.createDirectories(folder)
            fileSystem.write(pending) { writeUtf8(Json.encodeToString(report)) }
        } catch (_: IOException) {
        }
    }

    override suspend fun sendPending() {
        sendings.update { it + 1 }
        try {
            sendPendingNow()
        } finally {
            sendings.update { it - 1 }
        }
    }

    private suspend fun sendPendingNow() {
        val body = try {
            if (!fileSystem.exists(pending)) return
            fileSystem.read(pending) { readUtf8() }.also { fileSystem.delete(pending) }
        } catch (_: IOException) {
            return
        }
        val consent = onboarding.consent.first()
        if (!consent.policyAccepted || !consent.crashReports) {
            journal.write("crash report: not sent, sending is off")
            return
        }
        uploader.post(UploadTarget.CRASH_REPORT, body)
    }

    override suspend fun awaitSending() {
        sendings.first { it == 0 }
    }

    private companion object {
        const val FOLDER = "crash"
        const val PENDING_FILE = "pending.json"
        const val MAX_ERROR_LENGTH = 64 * 1024
        const val LOG_TAIL_BYTES = 100 * 1024
    }
}
