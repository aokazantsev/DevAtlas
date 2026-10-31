package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.startup.data.VerifiedManifestsImpl
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.themes.manifestOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeLibraryImplTest {

    private val repository = FakeInstalledThemeRepository()
    private val current = FakeCurrentThemeRepository()
    private val storage = FakeThemeStorage()
    private val progress = FakeProgressRepository()
    private val clock = FakeClock(millis = 5_000L)
    private val verified = VerifiedManifestsImpl()
    private val library = ThemeLibraryImpl(repository, current, storage, progress, clock, FakeJournal(), verified)

    private suspend fun install(vararg themes: String) = themes.forEachIndexed { index, id ->
        repository.save(installedOf(id, lastUsedAt = index.toLong()))
    }

    @Test
    fun applyMarksThemeCurrentAndRecentlyUsed() = runTest {
        install("a", "b")

        library.apply("a")

        assertEquals("a", current.currentThemeId.first())
        assertEquals(5_000L, repository.find("a")?.lastUsedAt)
        assertEquals("a", library.observeCurrent().first()?.id)
    }

    @Test
    fun resolveCurrentFallsBackToMostRecentlyUsed() = runTest {
        install("a", "b")
        current.setCurrent("gone")

        assertEquals("b", library.resolveCurrent()?.id)
        assertEquals("b", current.currentThemeId.first())
    }

    @Test
    fun resolveCurrentIsNullWithoutThemes() = runTest {
        assertNull(library.resolveCurrent())
        assertNull(current.currentThemeId.first())
    }

    @Test
    fun resetOnlyKeepsThemeAndClearsProgress() = runTest {
        install("a")
        current.setCurrent("a")
        progress.viewed["a"] = mutableSetOf("p1", "p2")

        val result = library.remove("a", deleteTheme = false, resetProgress = true)

        assertEquals(ThemeRemoval.ProgressReset, result)
        assertEquals(0, library.viewedCount("a"))
        assertEquals("a", repository.find("a")?.id)
        assertEquals("a", current.currentThemeId.first())
        assertEquals(emptyList(), storage.deleted)
    }

    @Test
    fun deleteOnlyKeepsProgress() = runTest {
        install("a", "b")
        current.setCurrent("b")
        progress.viewed["a"] = mutableSetOf("p1")

        val result = library.remove("a", deleteTheme = true, resetProgress = false)

        assertEquals(ThemeRemoval.ThemeDeleted, result)
        assertEquals(1, library.viewedCount("a"))
        assertEquals(listOf("a"), storage.deleted)
        assertNull(repository.find("a"))
        assertEquals("b", current.currentThemeId.first())
    }

    @Test
    fun deletingCurrentSwitchesToMostRecentlyUsed() = runTest {
        install("a", "b", "c")
        current.setCurrent("c")

        val result = library.remove("c", deleteTheme = true, resetProgress = false)

        assertEquals(ThemeRemoval.SwitchedTo("b"), result)
        assertEquals("b", current.currentThemeId.first())
        assertEquals(5_000L, repository.find("b")?.lastUsedAt)
    }

    @Test
    fun leftoverFilesDoNotStopDeletingCurrentTheme() = runTest {
        install("a", "b")
        current.setCurrent("b")
        storage.failure = okio.IOException("занято")

        val result = library.remove("b", deleteTheme = true, resetProgress = false)

        assertEquals(ThemeRemoval.SwitchedTo("a"), result)
        assertNull(repository.find("b"))
        assertEquals("a", current.currentThemeId.first())
    }

    @Test
    fun databaseFailureOnDeleteIsReportedAndKeepsTheme() = runTest {
        install("a")
        current.setCurrent("a")
        repository.databaseFailure = androidx.sqlite.SQLiteException("база занята")

        val result = library.remove("a", deleteTheme = true, resetProgress = false)

        assertTrue(result is ThemeRemoval.Failed)
        assertEquals(emptyList(), storage.deleted.toList())
        assertEquals("a", current.currentThemeId.first())
    }

    @Test
    fun deletingLastThemeLeavesNoCurrent() = runTest {
        install("a")
        current.setCurrent("a")

        val result = library.remove("a", deleteTheme = true, resetProgress = true)

        assertEquals(ThemeRemoval.NoThemesLeft, result)
        assertNull(current.currentThemeId.first())
        assertEquals(0, library.viewedCount("a"))
    }

    @Test
    fun failedDeleteKeepsProgressThatWasAskedToReset() = runTest {
        install("a")
        progress.viewed["a"] = mutableSetOf("p1")
        repository.databaseFailure = androidx.sqlite.SQLiteException("база занята")

        val result = library.remove("a", deleteTheme = true, resetProgress = true)

        assertTrue(result is ThemeRemoval.Failed)
        assertEquals(1, library.viewedCount("a"))
    }

    @Test
    fun failedResetAfterDeleteStillFinishesDeletion() = runTest {
        install("a", "b")
        current.setCurrent("b")
        progress.resetFailure = androidx.sqlite.SQLiteException("база занята")

        val result = library.remove("b", deleteTheme = true, resetProgress = true)

        assertEquals(ThemeRemoval.SwitchedTo("a"), result)
        assertNull(repository.find("b"))
        assertEquals(listOf("b"), storage.deleted)
    }

    @Test
    fun deletingThemeForgetsItsVerifiedManifest() = runTest {
        install("a", "b")
        verified.replace(mapOf("a" to manifestOf("a"), "b" to manifestOf("b")))

        library.remove("a", deleteTheme = true, resetProgress = false)

        assertNull(verified.find("a"))
        assertEquals("b", verified.find("b")?.id)
    }
}
