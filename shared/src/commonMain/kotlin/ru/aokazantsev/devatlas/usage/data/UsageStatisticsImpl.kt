package ru.aokazantsev.devatlas.usage.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.upload.domain.ServiceUploader
import ru.aokazantsev.devatlas.upload.domain.model.UploadTarget
import ru.aokazantsev.devatlas.usage.data.model.UsageDay
import ru.aokazantsev.devatlas.usage.data.model.UsageLedger
import ru.aokazantsev.devatlas.usage.data.model.UsagePacket
import ru.aokazantsev.devatlas.usage.domain.UsageStateSource
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class UsageStatisticsImpl(
    private val store: UsageLedgerStore,
    private val uploader: ServiceUploader,
    private val onboarding: OnboardingRepository,
    private val stateSource: UsageStateSource,
    private val appInfo: AppInfo,
    private val platform: PlatformBindings,
    private val scope: CoroutineScope,
) : UsageStatistics {

    private val mutex = Mutex()

    override fun hit(event: UsageEvent) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withLock {
                if (!allowed()) {
                    store.clear()
                    return@withLock
                }
                val ledger = store.load()
                val today = store.today()
                val day = ledger.days[today] ?: UsageDay(version = version())
                val counted = day.copy(
                    version = version(),
                    events = day.events + (event.key to (day.events[event.key] ?: 0) + 1),
                    revision = day.revision + 1,
                )
                store.save(UsageLedger(ledger.days + (today to counted)))
            }
        }
    }

    override fun forget() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { mutex.withLock { store.clear() } }
    }

    override suspend fun send() {
        val packets = mutex.withLock { pack() }
        for (packet in packets) {
            if (!mutex.withLock { allowed() }) return
            val status = uploader.post(UploadTarget.USAGE, packet.body)
            if (status !in FINAL_STATUSES) return
            mutex.withLock { markSent(packet) }
        }
    }

    private suspend fun markSent(packet: UsagePacket) {
        if (!allowed()) return
        val ledger = store.load()
        val day = ledger.days[packet.day]
        if (day != null && day.revision == packet.revision) {
            store.save(UsageLedger(ledger.days + (packet.day to day.copy(sentRevision = packet.revision))))
        }
    }

    private suspend fun pack(): List<UsagePacket> {
        if (!allowed()) {
            store.clear()
            return emptyList()
        }
        val today = store.today()
        val oldest = store.oldestKeptDay()
        val days = store.load().days.filterKeys { it >= oldest }
        store.save(UsageLedger(days))
        val state = stateSource.state()
        val id = store.installId()
        return days.filterValues { it.revision != it.sentRevision }.map { (key, day) ->
            UsagePacket(day = key, revision = day.revision, body = packet(id, key, day, if (key == today) state else emptyMap()))
        }
    }

    private fun packet(id: String, key: String, day: UsageDay, state: Map<String, String>): String = buildJsonObject {
        put("id", id)
        put("day", key)
        put("version", day.version)
        put("windows", androidVersion())
        put("events", buildJsonObject { day.events.forEach { (name, count) -> put(name, count) } })
        put("state", buildJsonObject { state.forEach { (name, value) -> put(name, value) } })
    }.toString()

    private suspend fun allowed(): Boolean = onboarding.consent.first().let { it.policyAccepted && it.usageStatistics }

    private fun version(): String = appInfo.versionName.substringBefore('-')

    private fun androidVersion(): String = platform.device.androidVersion.let { if ('.' in it) it else "$it.0" }

    private companion object {
        val FINAL_STATUSES = setOf(200, 400, 413)
    }
}
