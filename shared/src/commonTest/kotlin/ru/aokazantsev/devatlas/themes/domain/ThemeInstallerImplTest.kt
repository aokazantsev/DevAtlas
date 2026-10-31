package ru.aokazantsev.devatlas.themes.domain

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.IOException
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.startup.data.VerifiedManifestsImpl
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeCurrentThemeRepository
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePackageFetcher
import ru.aokazantsev.devatlas.themes.FakeProgressRepository
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.themes.manifestOf
import ru.aokazantsev.devatlas.themes.preparedOf
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeInstallerImplTest {

    private val storage = FakeThemeStorage()
    private val repository = FakeInstalledThemeRepository()
    private val progress = FakeProgressRepository()
    private val current = FakeCurrentThemeRepository()
    private val clock = FakeClock(millis = 9_000L)

    private val usage = FakeUsageStatistics()
    private val journal = FakeJournal()
    private val verified = VerifiedManifestsImpl()

    private fun installer(fetcher: ThemePackageFetcher = FakePackageFetcher()) = ThemeInstallerImpl(
        resolver = ThemeSourceResolverImpl(NetworkEndpoints.Production),
        fetcher = fetcher,
        storage = storage,
        repository = repository,
        progress = progress,
        current = current,
        clock = clock,
        usage = usage,
        journal = journal,
        verified = verified,
    )

    private fun installed(result: InstallResult) = assertIs<InstallResult.Installed>(result).theme

    @Test
    fun firstInstallBecomesCurrent() = runTest {
        val theme = installed(installer().install(preparedOf("a", "sha", "p1", "p2")))

        assertEquals(listOf("a"), storage.committed)
        assertEquals("a", current.currentThemeId.first())
        assertEquals(2, theme.pageCount)
        assertEquals("sha", repository.find("a")?.sha256)
        assertEquals(1, journal.lines.count { it.startsWith("theme install: ") && it.endsWith(": ok") })
    }

    @Test
    fun secondInstallDoesNotSwitchCurrentTheme() = runTest {
        current.setCurrent("a")
        repository.save(installedOf("a"))

        val theme = installed(installer().install(preparedOf("b", "sha", "p1")))

        assertEquals("a", current.currentThemeId.first())
        assertEquals(0L, theme.lastUsedAt)
    }

    @Test
    fun firstThemeIsMarkedUsedAtInstall() = runTest {
        assertEquals(9_000L, installed(installer().install(preparedOf("a", "sha", "p1"))).lastUsedAt)
    }

    @Test
    fun diskFailureIsReportedAndLeavesStateUntouched() = runTest {
        storage.failure = IOException("нет места")

        val result = installer().install(preparedOf("a", "sha", "p1"))

        assertTrue(assertIs<InstallResult.Failed>(result).failure.message.contains("нет места"))
        assertNull(repository.find("a"))
        assertNull(current.currentThemeId.first())
        assertEquals(1, storage.discarded.size)
        assertTrue(journal.lines.single().let { it.startsWith("theme install: ") && it.contains("failed: ") })
    }

    @Test
    fun databaseFailureAfterMovingFilesRollsThemBack() = runTest {
        repository.saveFailure = SQLiteException("нет места в базе")

        val result = installer().install(preparedOf("a", "sha", "p1"))

        assertIs<InstallResult.Failed>(result)
        assertEquals(listOf("a"), storage.committed)
        assertEquals(listOf("a"), storage.rolledBack)
        assertNull(current.currentThemeId.first())
        assertEquals(1, storage.discarded.size)
    }

    @Test
    fun progressFailureAfterSavingRestoresPreviousRecord() = runTest {
        val previous = installedOf("a", lastUsedAt = 5L)
        repository.save(previous)
        progress.keepFailure = SQLiteException("нет места в базе")

        val result = installer().install(preparedOf("a", "new", "p1"))

        assertIs<InstallResult.Failed>(result)
        assertEquals(previous, repository.find("a"))
        assertEquals(listOf("a"), storage.rolledBack)
    }

    @Test
    fun progressFailureOnFirstInstallLeavesNoRecord() = runTest {
        progress.keepFailure = SQLiteException("нет места в базе")

        installer().install(preparedOf("a", "new", "p1"))

        assertNull(repository.find("a"))
        assertNull(current.currentThemeId.first())
    }

    @Test
    fun redownloadReportsDiskFailure() = runTest {
        repository.save(installedOf("a"))
        storage.failure = IOException("нет места")
        val fetcher = FakePackageFetcher(FetchResult.Fetched(preparedOf("a", "fresh", "p1")))

        val failure = installer(fetcher).redownload("a")

        assertTrue(failure?.message?.contains("нет места") == true)
        assertEquals("sha-a", repository.find("a")?.sha256)
    }

    @Test
    fun replacingDropsProgressOfVanishedPagesAndKeepsDates() = runTest {
        repository.save(installedOf("a", lastUsedAt = 77L))
        progress.viewed["a"] = mutableSetOf("p1", "p3")

        val theme = installed(installer().install(preparedOf("a", "new", "p1", "p2")))

        assertEquals(setOf("p1"), progress.viewed.getValue("a").toSet())
        assertEquals(77L, theme.lastUsedAt)
    }

    @Test
    fun prepareRejectsBadAddressWithHelpLink() = runTest {
        val result = installer().prepare("http://example.com/theme.zip")

        val failed = assertIs<PrepareResult.Failed>(result)
        assertTrue(failed.failure.message.contains("https"))
        assertTrue(failed.failure.helpUrl?.endsWith("#publish") == true)
    }

    @Test
    fun prepareReturnsFetchedTheme() = runTest {
        val prepared = preparedOf("a", "sha", "p1")
        val fetcher = FakePackageFetcher(FetchResult.Fetched(prepared))

        val result = installer(fetcher).prepare("https://example.com/a.zip")

        assertEquals(prepared, assertIs<PrepareResult.Ready>(result).prepared)
    }

    @Test
    fun prepareForwardsFetcherFailure() = runTest {
        val failure = ThemeFailure("битый архив")
        val fetcher = FakePackageFetcher(FetchResult.Failed(failure))

        val result = installer(fetcher).prepare("https://example.com/a.zip")

        assertEquals(failure, assertIs<PrepareResult.Failed>(result).failure)
    }

    @Test
    fun redownloadReplacesSameTheme() = runTest {
        repository.save(installedOf("a"))
        val fetcher = FakePackageFetcher(FetchResult.Fetched(preparedOf("a", "fresh", "p1")))

        val failure = installer(fetcher).redownload("a")

        assertNull(failure)
        assertEquals("fresh", repository.find("a")?.sha256)
        assertEquals(listOf("a"), storage.committed)
        assertEquals(emptyList(), usage.events)
        assertEquals(1, journal.lines.count { it.startsWith("theme redownload: ") && it.endsWith(": ok") })
    }

    @Test
    fun updateIsCountedAndForgetsVerifiedManifest() = runTest {
        repository.save(installedOf("a"))
        verified.replace(mapOf("a" to manifestOf("a")))

        installer().install(preparedOf("a", "new", "p1"))

        assertEquals(listOf(UsageEvent.THEME_UPDATE), usage.events)
        assertNull(verified.find("a"))
    }

    @Test
    fun rejectedAddressIsJournaled() = runTest {
        installer().prepare("http://example.com/theme.zip")

        assertTrue(journal.lines.single().let { it.startsWith("theme source: ") && it.contains("rejected") })
    }

    @Test
    fun redownloadRefusesAnotherThemeAtTheSameLink() = runTest {
        repository.save(installedOf("a"))
        val fetcher = FakePackageFetcher(FetchResult.Fetched(preparedOf("other", "x", "p1")))

        val failure = installer(fetcher).redownload("a")

        assertTrue(failure?.message?.contains("другая тема") == true)
        assertEquals(emptyList(), storage.committed)
        assertEquals(1, storage.discarded.size)
        assertEquals("sha-a", repository.find("a")?.sha256)
        assertTrue(journal.lines.any { it.startsWith("theme redownload: ") && it.contains("another theme other") })
    }

    @Test
    fun redownloadOfUnknownThemeFails() = runTest {
        val failure = installer().redownload("missing")

        assertEquals("Тема уже удалена", failure?.message)
    }
}
