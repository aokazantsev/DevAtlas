package ru.aokazantsev.devatlas.themes.domain

import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themes.FakeClock
import ru.aokazantsev.devatlas.themes.FakeInstalledThemeRepository
import ru.aokazantsev.devatlas.themes.FakePackageFetcher
import ru.aokazantsev.devatlas.themes.FakePackageReader
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeUpdaterImplTest {

    private val storage = FakeThemeStorage()
    private val reader = FakePackageReader()
    private val repository = FakeInstalledThemeRepository()

    private fun updater(fetcher: FakePackageFetcher) = ThemeUpdaterImpl(
        resolver = ThemeSourceResolverImpl(NetworkEndpoints.Production),
        fetcher = fetcher,
        storage = storage,
        reader = reader,
        repository = repository,
        clock = FakeClock(),
    )

    @Test
    fun notModifiedMeansNoChangesAndRemembersCheck() = runTest {
        repository.save(installedOf("a"))
        val fetcher = FakePackageFetcher(FetchResult.NotModified)

        val result = updater(fetcher).check("a")

        assertEquals(UpdateCheck.NoChanges, result)
        assertEquals(listOf("a"), repository.checked)
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
