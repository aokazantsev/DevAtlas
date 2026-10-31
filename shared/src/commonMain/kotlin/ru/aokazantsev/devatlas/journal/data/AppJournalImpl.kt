package ru.aokazantsev.devatlas.journal.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.Buffer
import okio.ByteString
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import okio.use
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import kotlin.jvm.Synchronized
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class AppJournalImpl(
    private val fileSystem: FileSystem,
    private val clock: Clock,
    platform: PlatformBindings,
) : AppJournal {

    private val folder = platform.dataDirectory / FOLDER
    private val current = folder / CURRENT_FILE
    private val previous = folder / PREVIOUS_FILE

    @Synchronized
    override fun write(message: String) {
        val singleLine = message.replace('\r', ' ').replace('\n', ' ').take(MAX_LINE_LENGTH)
        append("${clock.now()} $singleLine\n")
    }

    @Synchronized
    override fun tail(maxBytes: Int): String {
        val joined = Buffer().write(read(previous)).write(read(current))
        if (joined.size > maxBytes) {
            joined.skip(joined.size - maxBytes)
            val lineEnd = joined.indexOf(NEW_LINE)
            if (lineEnd >= 0) joined.skip(lineEnd + 1)
        }
        return joined.readUtf8()
    }

    private fun append(line: String) {
        try {
            fileSystem.createDirectories(folder)
            val size = fileSystem.metadataOrNull(current)?.size ?: 0L
            if (size >= MAX_FILE_BYTES) {
                fileSystem.delete(previous, mustExist = false)
                fileSystem.atomicMove(current, previous)
            }
            fileSystem.appendingSink(current, mustExist = false).buffer().use { it.writeUtf8(line) }
        } catch (_: IOException) {
        }
    }

    private fun read(path: Path): ByteString = try {
        if (fileSystem.exists(path)) fileSystem.read(path) { readByteString() } else ByteString.EMPTY
    } catch (_: IOException) {
        ByteString.EMPTY
    }

    private companion object {
        const val FOLDER = "journal"
        const val CURRENT_FILE = "journal.log"
        const val PREVIOUS_FILE = "journal.1.log"
        const val MAX_FILE_BYTES = 256L * 1024
        const val MAX_LINE_LENGTH = 2_000
        const val NEW_LINE = '\n'.code.toByte()
    }
}
