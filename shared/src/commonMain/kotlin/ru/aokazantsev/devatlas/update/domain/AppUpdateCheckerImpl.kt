package ru.aokazantsev.devatlas.update.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import ru.aokazantsev.devatlas.update.domain.model.ReleaseList
import ru.aokazantsev.devatlas.update.domain.model.ReleasesResult
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@ContributesBinding(AppScope::class)
@Inject
internal class AppUpdateCheckerImpl(
    private val releases: ReleasesRepository,
    private val settings: UpdateSettingsRepository,
    private val appInfo: AppInfo,
    private val usage: UsageStatistics,
    private val journal: AppJournal,
    private val clock: Clock,
) : AppUpdateChecker {

    override suspend fun check(): UpdateCheck {
        usage.hit(UsageEvent.UPDATE_CHECK)
        val check = when (val result = releases.load()) {
            is ReleasesResult.Loaded -> judge(result.list)
            is ReleasesResult.Failed -> {
                val cached = result.cached?.let(::judge) as? UpdateCheck.Available
                cached?.copy(offlineNote = "${result.message}. Список изменений — с прошлой проверки")
                    ?: UpdateCheck.Failed(result.message)
            }
        }
        journal.write("update check: ${check.describe()}")
        return check
    }

    override suspend fun checkIfDue(): UpdateCheck.Available? {
        val now = clock.now().toEpochMilliseconds()
        val due = settings.autoCheck.first() && now - settings.lastAutoCheckMillis() !in 0 until CHECK_INTERVAL_MILLIS
        val check = if (due) autoCheck(now) else null
        return check?.takeIf { it.latest.version.toString() != settings.announcedVersion() }
    }

    private suspend fun autoCheck(now: Long): UpdateCheck.Available? {
        usage.hit(UsageEvent.UPDATE_CHECK)
        val result = releases.load()
        if (result !is ReleasesResult.Loaded) {
            journal.write("auto update check: failed")
            return null
        }
        settings.setLastAutoCheckMillis(now)
        val check = judge(result.list)
        journal.write("auto update check: ${check.describe()}")
        return check as? UpdateCheck.Available
    }

    override suspend fun markAnnounced(check: UpdateCheck.Available) {
        settings.setAnnouncedVersion(check.latest.version.toString())
    }

    private fun judge(list: ReleaseList): UpdateCheck {
        val installed = AppVersion.parse(appInfo.versionName) ?: AppVersion(listOf(0))
        val stable = list.releases.sortedByDescending { it.version }
        val latest = stable.firstOrNull()
        if (latest == null || latest.version <= installed) return UpdateCheck.UpToDate(installed.toString())
        val changes = stable.filter { it.version > installed }
        return UpdateCheck.Available(
            latest = latest,
            changes = changes,
            hasOlderChanges = !list.complete && changes.size == stable.size,
        )
    }

    private fun UpdateCheck.describe(): String = when (this) {
        is UpdateCheck.UpToDate -> "up to date $installed"
        is UpdateCheck.Available -> "found ${latest.version}" + (offlineNote?.let { " from cache" } ?: "")
        is UpdateCheck.Failed -> "failed: $message"
    }

    private companion object {
        val CHECK_INTERVAL_MILLIS = 1.days.inWholeMilliseconds
    }
}
