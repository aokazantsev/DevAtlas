package ru.aokazantsev.devatlas.usage.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.IOException
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.usage.data.model.UsageLedger
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@ContributesBinding(AppScope::class)
@Inject
internal class UsageLedgerStoreImpl(
    private val fileSystem: FileSystem,
    private val platform: PlatformBindings,
    private val clock: Clock,
) : UsageLedgerStore {

    private val folder = platform.dataDirectory / FOLDER
    private val ledgerPath = folder / LEDGER_FILE
    private val installIdPath = folder / INSTALL_ID_FILE
    private val json = Json { ignoreUnknownKeys = true }

    override fun today(): String = dayOf(clock.now())

    override fun oldestKeptDay(): String = dayOf(clock.now() - (KEEP_DAYS - 1).days)

    override fun load(): UsageLedger = try {
        if (fileSystem.exists(ledgerPath)) json.decodeFromString(fileSystem.read(ledgerPath) { readUtf8() }) else UsageLedger()
    } catch (_: IOException) {
        UsageLedger()
    } catch (_: SerializationException) {
        UsageLedger()
    } catch (_: IllegalArgumentException) {
        UsageLedger()
    }

    override fun save(ledger: UsageLedger) {
        try {
            fileSystem.createDirectories(folder)
            val temporary = folder / "$LEDGER_FILE.tmp"
            fileSystem.write(temporary) { writeUtf8(json.encodeToString(ledger)) }
            fileSystem.atomicMove(temporary, ledgerPath)
        } catch (_: IOException) {
        }
    }

    override fun clear() {
        try {
            fileSystem.delete(ledgerPath, mustExist = false)
        } catch (_: IOException) {
        }
    }

    override fun installId(): String {
        val stored = try {
            if (fileSystem.exists(installIdPath)) fileSystem.read(installIdPath) { readUtf8() }.trim() else null
        } catch (_: IOException) {
            null
        }
        if (stored != null && INSTALL_ID.matches(stored)) return stored
        val created = Random.nextBytes(INSTALL_ID_BYTES).joinToString("") { byte ->
            (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
        }
        try {
            fileSystem.createDirectories(folder)
            fileSystem.write(installIdPath) { writeUtf8(created) }
        } catch (_: IOException) {
        }
        return created
    }

    private fun dayOf(instant: Instant): String {
        val millis = instant.toEpochMilliseconds()
        return Instant.fromEpochMilliseconds(millis + platform.timeZoneOffsetMillis(millis)).toString().take(DAY_LENGTH)
    }

    private companion object {
        const val FOLDER = "usage"
        const val LEDGER_FILE = "days.json"
        const val INSTALL_ID_FILE = "install-id"
        const val KEEP_DAYS = 7
        const val DAY_LENGTH = 10
        const val INSTALL_ID_BYTES = 16
        const val BYTE_MASK = 0xFF
        const val HEX_RADIX = 16
        val INSTALL_ID = Regex("[0-9a-f]{32}")
    }
}
