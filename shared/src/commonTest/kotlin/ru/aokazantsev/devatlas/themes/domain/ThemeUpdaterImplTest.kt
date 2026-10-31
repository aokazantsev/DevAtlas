package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePackageFetcher
import ru.aokazantsev.devatlas.themes.FakePackageReader
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.themes.domain.model.UpdateSummary
import ru.aokazantsev.devatlas.themes.installedOf
import ru.aokazantsev.devatlas.themes.manifestOf
import ru.aokazantsev.devatlas.themes.preparedOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeUpdaterImplTest {

    private val storage = FakeThemeStorage()
    private val reader = FakePackageReader()
    private val repository = FakeInstalledThemeRepository()
    private val journal = FakeJournal()

    private fun updater(fetcher: FakePackageFetcher) = ThemeUpdaterImpl(
        resolver = ThemeSourceResolverImpl(NetworkEndpoints.Production),
        fetcher = fetcher,
        storage = storage,
        reader = reader,
        repository = repository,
        journal = journal,
    )

    @Test
    fun notModifiedMeansNoChanges() = runTest {
        repository.save(installedOf("a"))
        val fetcher = FakePackageFetcher(FetchResult.NotModified)

        val result = updater(fetcher).check("a")

        assertEquals(UpdateCheck.NoChanges, result)
        assertEquals(CacheValidators("etag-a", null), fetcher.requests.single().second)
    }

    @Test
    fun sameHashMeansNoChangesAndDiscardsDownload() = runTest {
        repository.save(installedOf("a", sha256 = "same"))
        val fetcher = FakePackageFetcher(FetchResult.Fetched(preparedOf("a", "same", "p1")))

        val result = updater(fetcher).check("a")

        assertEquals(UpdateCheck.NoChanges, result)
        assertEquals(1, storage.discarded.size)
    }

    @Test
    fun anotherThemeAtTheLinkIsRefused() = runTest {
        repository.save(installedOf("a"))
        val fetcher = FakePackageFetcher(FetchResult.Fetched(preparedOf("b", "x", "p1")))

        val result = updater(fetcher).check("a")

        val failed = assertIs<UpdateCheck.Failed>(result)
        assertTrue(failed.failure.message.contains("другая тема"))
        assertEquals(1, storage.discarded.size)
        assertTrue(journal.lines.single().contains("another theme b"))
    }

    @Test
    fun sameHashWithNewValidatorsRemembersThem() = runTest {
        repository.save(installedOf("a", sha256 = "same"))
        val prepared = preparedOf("a", "same", "p1").copy(etag = "etag-new", lastModified = "Sat, 31 Oct 2026 10:00:00 GMT")

        val result = updater(FakePackageFetcher(FetchResult.Fetched(prepared))).check("a")

        assertEquals(UpdateCheck.NoChanges, result)
        val saved = repository.find("a")
        assertEquals("etag-new", saved?.etag)
        assertEquals("Sat, 31 Oct 2026 10:00:00 GMT", saved?.lastModified)
    }

    @Test
    fun failureToRememberValidatorsStillMeansNoChanges() = runTest {
        repository.save(installedOf("a", sha256 = "same"))
        repository.saveFailure = androidx.sqlite.SQLiteException("база занята")
        val prepared = preparedOf("a", "same", "p1").copy(etag = "etag-new")

        val result = updater(FakePackageFetcher(FetchResult.Fetched(prepared))).check("a")

        assertEquals(UpdateCheck.NoChanges, result)
        assertEquals("etag-a", repository.find("a")?.etag)
    }

    @Test
    fun databaseFailureOnFindIsReported() = runTest {
        repository.findFailure = androidx.sqlite.SQLiteException("база занята")
        val fetcher = FakePackageFetcher()

        val result = updater(fetcher).check("a")

        assertTrue(assertIs<UpdateCheck.Failed>(result).failure.message.contains("базу"))
        assertTrue(fetcher.requests.isEmpty())
    }

    @Test
    fun cancellationWhileSummarizingDiscardsDownload() = runTest {
        repository.save(installedOf("a", sha256 = "old"))
        val prepared = preparedOf("a", "new", "p1")
        reader.cancelOnHashes = true

        assertFailsWith<CancellationException> { updater(FakePackageFetcher(FetchResult.Fetched(prepared))).check("a") }

        assertEquals(listOf(prepared.staging), storage.discarded.toList())
    }

    @Test
    fun summaryCountsAddedRemovedAndChangedByHash() = runTest {
        repository.save(installedOf("a", sha256 = "old"))
        val prepared = preparedOf("a", "new", "keep", "edited", "fresh")
        reader.manifests[storage.themeDirectory("a")] = manifestOf("a", "A", "keep", "edited", "gone", "gone2")
        reader.hashes[storage.themeDirectory("a")] = mapOf("keep" to "1", "edited" to "2", "gone" to "3", "gone2" to "4")
        reader.hashes[prepared.staging.content] = mapOf("keep" to "1", "edited" to "22", "fresh" to "5")

        val result = updater(FakePackageFetcher(FetchResult.Fetched(prepared))).check("a")

        val available = assertIs<UpdateCheck.Available>(result)
        assertEquals(UpdateSummary(added = 1, removed = 2, changed = 1), available.summary)
        assertEquals(true, available.contentChanged)
    }

    @Test
    fun summaryCountsRemovedSectionsEvenWhenPagesAreUnchanged() = runTest {
        repository.save(installedOf("a", sha256 = "old"))
        val prepared = preparedOf("a", "new", "p1")
        val directory = storage.themeDirectory("a")
        reader.manifests[directory] = manifestOf("a", "A", "p1").let { manifest ->
            manifest.copy(
                tree = manifest.tree + ThemeNode.Section(
                    id = "s1",
                    title = "s1",
                    children = listOf(ThemeNode.Section(id = "s2", title = "s2", children = emptyList())),
                ),
            )
        }
        reader.hashes[directory] = mapOf("p1" to "1")
        reader.hashes[prepared.staging.content] = mapOf("p1" to "1")

        val result = updater(FakePackageFetcher(FetchResult.Fetched(prepared))).check("a")

        val summary = assertIs<UpdateCheck.Available>(result).summary
        assertEquals(UpdateSummary(added = 0, removed = 0, changed = 0, removedSections = 2), summary)
    }

    @Test
    fun newAddressSkipsValidatorsAndIsAvailableEvenWithSameHash() = runTest {
        repository.save(installedOf("a", sha256 = "same"))
        val prepared = preparedOf("a", "same", "p1")
        val fetcher = FakePackageFetcher(FetchResult.Fetched(prepared))

        val result = updater(fetcher).check("a", newAddress = "https://example.com/other/a.zip")

        assertEquals(false, assertIs<UpdateCheck.Available>(result).contentChanged)
        assertNull(fetcher.requests.single().second)
        assertEquals("https://example.com/other/a.zip", fetcher.requests.single().first.address)
    }

    @Test
    fun badNewAddressFailsWithoutDownloading() = runTest {
        repository.save(installedOf("a"))
        val fetcher = FakePackageFetcher()

        val result = updater(fetcher).check("a", newAddress = "https://drive.google.com/file/d/1/view")

        assertTrue(assertIs<UpdateCheck.Failed>(result).failure.message.contains("Google"))
        assertTrue(fetcher.requests.isEmpty())
        assertTrue(journal.lines.single().contains("rejected"))
    }

    @Test
    fun fetchFailureIsForwarded() = runTest {
        repository.save(installedOf("a"))
        val failure = ThemeFailure("нет сети")

        val result = updater(FakePackageFetcher(FetchResult.Failed(failure))).check("a")

        assertEquals(UpdateCheck.Failed(failure), result)
    }

    @Test
    fun unknownThemeFails() = runTest {
        val result = updater(FakePackageFetcher()).check("missing")

        assertIs<UpdateCheck.Failed>(result)
    }
}
