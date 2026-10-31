package ru.aokazantsev.devatlas.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.crash.domain.CrashReports
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.update.domain.UpdateApkStorage
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
@Suppress("LongParameterList")
internal class AppServicesImpl(
    private val crashReports: CrashReports,
    private val usage: UsageStatistics,
    private val scheduler: UsageScheduler,
    private val journal: AppJournal,
    private val appInfo: AppInfo,
    private val platform: PlatformBindings,
    private val updateApks: UpdateApkStorage,
    private val scope: CoroutineScope,
) : AppServices {

    private var started = false

    override fun start(freshLaunch: Boolean) {
        if (started && !freshLaunch) return
        started = true
        journal.write("start ${appInfo.versionName}, Android ${platform.device.androidVersion}, ${platform.device.model}")
        usage.hit(UsageEvent.APP_START)
        scheduler.schedule()
        scope.launch {
            updateApks.discardInstalled()
            crashReports.sendPending()
            usage.send()
        }
    }
}
