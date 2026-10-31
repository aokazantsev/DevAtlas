package ru.aokazantsev.devatlas.themes

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themes.domain.CurrentThemeRepository
import ru.aokazantsev.devatlas.themes.domain.InstalledThemeRepository
import ru.aokazantsev.devatlas.themes.domain.ProgressRepository
import ru.aokazantsev.devatlas.themes.domain.ThemePackageFetcher
import ru.aokazantsev.devatlas.themes.domain.ThemePackageReader
import ru.aokazantsev.devatlas.themes.domain.ThemeStorage
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.StagingArea
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan
import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(var millis: Long = 1_000L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
}

class FakeInstalledThemeRepository : InstalledThemeRepository {
    var saveFailure: SQLiteException? = null
    var databaseFailure: Exception? = null
    var findFailure: SQLiteException? = null
    var observeFailure: SQLiteException? = null
    val themes = MutableStateFlow<List<InstalledTheme>>(emptyList())

    override fun observeAll(): Flow<List<InstalledTheme>> = themes.map { list ->
        observeFailure?.let { throw it }
        list.sortedByDescending { it.lastUsedAt }
    }

    override suspend fun all(): List<InstalledTheme> {
        databaseFailure?.let { throw it }
        return themes.value.sortedByDescending { it.lastUsedAt }
    }

    override suspend fun find(themeId: String): InstalledTheme? {
        findFailure?.let { throw it }
        return themes.value.firstOrNull { it.id == themeId }
    }

    override suspend fun save(theme: InstalledTheme) {
        saveFailure?.let { throw it }
        themes.value = themes.value.filterNot { it.id == theme.id } + theme
    }

    override suspend fun delete(themeId: String) {
        databaseFailure?.let { throw it }
        themes.value = themes.value.filterNot { it.id == themeId }
    }

    override suspend fun touch(themeId: String, now: Long) {
        databaseFailure?.let { throw it }
        themes.value = themes.value.map { if (it.id == themeId) it.copy(lastUsedAt = now) else it }
    }

    override suspend fun updateValidators(themeId: String, sha256: String, etag: String?, lastModified: String?) {
        saveFailure?.let { throw it }
        themes.value = themes.value.map {
            if (it.id == themeId && it.sha256 == sha256) it.copy(etag = etag, lastModified = lastModified) else it
        }
    }
}

class FakeCurrentThemeRepository(initial: String? = null) : CurrentThemeRepository {
    private val state = MutableStateFlow(initial)
    override val currentThemeId: Flow<String?> = state

    override suspend fun setCurrent(themeId: String?) {
        state.value = themeId
    }
}

class FakeProgressRepository : ProgressRepository {
    var keepFailure: SQLiteException? = null
    val viewed = mutableMapOf<String, MutableSet<String>>()
    val bookmarks = mutableMapOf<String, Int>()
    private val changes = MutableStateFlow(0)

    override fun observeViewed(themeId: String): Flow<Set<String>> = changes.map { viewed[themeId].orEmpty().toSet() }

    override suspend fun setViewed(themeId: String, pageId: String, viewed: Boolean) {
        val pages = this.viewed.getOrPut(themeId) { mutableSetOf() }
        if (viewed) pages += pageId else pages -= pageId
        changes.value++
    }

    var countFailure: SQLiteException? = null

    override suspend fun viewedCount(themeId: String): Int {
        countFailure?.let { throw it }
        return viewed[themeId]?.size ?: 0
    }

    override suspend fun bookmarkCount(themeId: String): Int = bookmarks[themeId] ?: 0

    var resetFailure: SQLiteException? = null

    override suspend fun reset(themeId: String) {
        resetFailure?.let { throw it }
        viewed.remove(themeId)
        bookmarks.remove(themeId)
        changes.value++
    }

    override suspend fun keepOnly(themeId: String, manifest: ThemeManifest) {
        keepFailure?.let { throw it }
        viewed[themeId]?.retainAll(manifest.pages.map { it.id }.toSet())
        changes.value++
    }
}

class FakeThemeStorage : ThemeStorage {
    var failure: IOException? = null
    var stagingFailure: IOException? = null
    val committed = mutableListOf<String>()
    val discarded = mutableListOf<StagingArea>()
    val deleted = mutableListOf<String>()
    private var counter = 0

    override fun newStaging(): StagingArea {
        stagingFailure?.let { throw it }
        val directory = "/staging/${counter++}".toPath()
        return StagingArea(directory, directory / "archive.zip", directory / "content")
    }

    override fun themeDirectory(themeId: String): Path = "/themes/$themeId".toPath()

    val rolledBack = mutableListOf<String>()

    override suspend fun rollback(staging: StagingArea, themeId: String) {
        rolledBack += themeId
    }

    override suspend fun commit(staging: StagingArea, themeId: String) {
        failure?.let { throw it }
        committed += themeId
    }

    override fun discard(staging: StagingArea) {
        discarded += staging
    }

    override suspend fun deleteTheme(themeId: String) {
        failure?.let { throw it }
        deleted += themeId
    }

    var cleared = 0

    override fun clearStaging() {
        cleared++
    }
}

class FakePackageReader : ThemePackageReader {
    val manifests = mutableMapOf<Path, ThemeManifest>()
    val hashes = mutableMapOf<Path, Map<String, String>>()

    override fun manifest(themeDirectory: Path): ThemeManifest? = manifests[themeDirectory]

    var cancelOnHashes = false

    override fun pageHashes(themeDirectory: Path, manifest: ThemeManifest): Map<String, String> {
        if (cancelOnHashes) throw kotlinx.coroutines.CancellationException("отменено")
        return hashes[themeDirectory].orEmpty()
    }

    override fun contentDigest(themeDirectory: Path): String = "digest:$themeDirectory"
}

class FakePackageFetcher(vararg results: FetchResult) : ThemePackageFetcher {
    private val queue = ArrayDeque(results.toList())
    val requests = mutableListOf<Pair<ThemeSourcePlan, CacheValidators?>>()

    override suspend fun fetch(plan: ThemeSourcePlan, validators: CacheValidators?): FetchResult {
        requests += plan to validators
        return queue.removeFirst()
    }
}

fun manifestOf(id: String, title: String = id, vararg pageIds: String) = ThemeManifest(
    formatVersion = 1,
    id = id,
    title = title,
    description = null,
    tree = pageIds.map { ThemeNode.Page(id = it, title = it, file = "$it.html") },
)

fun preparedOf(
    id: String,
    sha256: String = "sha-$id",
    vararg pageIds: String,
    staging: StagingArea = StagingArea("/staging/x".toPath(), "/staging/x/archive.zip".toPath(), "/staging/x/content".toPath()),
) = PreparedTheme(
    staging = staging,
    manifest = manifestOf(id, "Тема $id", *pageIds),
    source = "https://example.com/$id.zip",
    etag = "etag-$id",
    lastModified = null,
    sha256 = sha256,
    sizeBytes = 1_000L,
)

fun installedOf(id: String, lastUsedAt: Long = 0L, sha256: String = "sha-$id") = InstalledTheme(
    id = id,
    title = "Тема $id",
    description = null,
    sourceUrl = "https://example.com/$id.zip",
    etag = "etag-$id",
    lastModified = null,
    sha256 = sha256,
    pageCount = 1,
    sizeBytes = 1_000L,
    lastUsedAt = lastUsedAt,
)
