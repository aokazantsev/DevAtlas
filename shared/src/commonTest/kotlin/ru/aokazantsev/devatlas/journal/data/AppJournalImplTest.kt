package ru.aokazantsev.devatlas.journal.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakePlatformBindings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppJournalImplTest {

    private val fileSystem = FakeFileSystem()
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

    private fun journal() = AppJournalImpl(
        fileSystem = fileSystem,
        clock = FakeClock(),
        scope = scope,
        platform = FakePlatformBindings(),
    )

    @Test
    fun lastLineBeforeCrashIsInTail() {
        val journal = journal()

        journal.write("перед сбоем")

        assertTrue(journal.tail(MAX_TAIL).endsWith("перед сбоем\n"))
    }

    @Test
    fun linesGoToFileOneLinePerMessage() = runTest {
        val journal = journal()

        journal.write("first\nsecond")
        journal.write("third")

        val lines = fileSystem.read(CURRENT) { readUtf8() }.lines().filter(String::isNotEmpty)
        assertEquals(2, lines.size)
        assertTrue(lines[0].endsWith("first second"))
        assertTrue(journal.tail(MAX_TAIL).endsWith("third\n"))
    }

    @Test
    fun fullFileRotatesAndTailSpansBothFiles() = runTest {
        val journal = journal()
        val line = "x".repeat(1_000)

        repeat(300) { journal.write("$it $line") }

        assertTrue(fileSystem.exists(PREVIOUS))
        assertTrue(fileSystem.metadata(CURRENT).size!! < 256L * 1024)
        val tail = journal.tail(MAX_TAIL)
        assertTrue(tail.encodeToByteArray().size in (MAX_TAIL - LINE_BYTES)..MAX_TAIL)
        assertTrue(tail.startsWith(FakeClock().now().toString()))
        assertTrue(tail.endsWith("299 $line\n"))
    }

    @Test
    fun writesFromMainThreadAreDeferredButKeepOrderInTail() = runTest {
        val idle = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        idle.cancel()
        scope = idle
        val journal = journal()

        journal.write("one")
        journal.write("two")
        journal.write("three")

        assertTrue(!fileSystem.exists(CURRENT))
        val lines = journal.tail(MAX_TAIL).lines().filter(String::isNotEmpty)
        assertEquals(listOf("one", "two", "three"), lines.map { it.substringAfter(' ') })
        assertEquals(3, fileSystem.read(CURRENT) { readUtf8() }.lines().count(String::isNotEmpty))
    }

    private companion object {
        const val MAX_TAIL = 100 * 1024
        const val LINE_BYTES = 1_100
        val CURRENT = "/data/journal/journal.log".toPath()
        val PREVIOUS = "/data/journal/journal.1.log".toPath()
    }
}
