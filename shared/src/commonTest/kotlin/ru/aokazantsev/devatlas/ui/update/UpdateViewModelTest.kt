package ru.aokazantsev.devatlas.ui.update

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path
import okio.Path.Companion.toPath
import ru.aokazantsev.devatlas.files.FakeExternalApps
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUpdateApkStorage
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.update.domain.ApkDownloader
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.update.domain.AppUpdateChecker
import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import ru.aokazantsev.devatlas.update.domain.model.ReleaseApk
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val scopes = ViewModelScopes()
    private val usage = FakeUsageStatistics()
    private val release = AppRelease(AppVersion(listOf(0, 2)), bodyHtml = "<p>Новое</p>", apk = null)
    private var check: UpdateCheck = UpdateCheck.Available(release, listOf(release), hasOlderChanges = false)
    private var downloads = 0
    private var download = CompletableDeferred<ApkDownloadResult>()
    private val installer = FakeInstaller()
    private val apks = FakeUpdateApkStorage()
    private var lateProgress: ((Long, Long?) -> Unit)? = null

    private val checker = object : AppUpdateChecker {
        override suspend fun check(): UpdateCheck = check

        override suspend fun checkIfDue(): UpdateCheck.Available? = null

        override suspend fun markAnnounced(check: UpdateCheck.Available) = Unit
    }

    private val downloader = object : ApkDownloader {
        override suspend fun download(release: AppRelease, onProgress: (Long, Long?) -> Unit): ApkDownloadResult {
            downloads++
            lateProgress = onProgress
            onProgress(1L, 2L)
            return download.await()
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    private fun newViewModel() = UpdateViewModel(
        checker = checker,
        downloader = downloader,
        installer = installer,
        externalApps = FakeExternalApps(),
        usage = usage,
        journal = FakeJournal(),
        apks = apks,
        scope = TestScope(UnconfinedTestDispatcher()),
    )

    private fun viewModel() = scopes.track(newViewModel())

    private val UpdateViewModel.step: UpdateStep
        get() = assertIs<UpdateUiState.Available>(state.value).step

    @Test
    fun availableUpdateShowsParsedNotes() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val shown = assertIs<UpdateUiState.Available>(viewModel.state.value)
        assertEquals(listOf("0.2"), shown.notes.map { it.version })
        assertEquals(1, shown.notes.single().blocks.size)
    }

    @Test
    fun cancelReturnsToUpdateButton() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.update()
        advanceUntilIdle()
        assertEquals(UpdateStep.Downloading(1L, 2L), viewModel.step)
        viewModel.cancelDownload()
        advanceUntilIdle()

        assertEquals(UpdateStep.Idle, viewModel.step)
    }

    @Test
    fun lateProgressAfterCancelDoesNotOverwriteIdle() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.update()
        advanceUntilIdle()
        viewModel.cancelDownload()

        lateProgress?.invoke(2L, 2L)

        assertEquals(UpdateStep.Idle, viewModel.step)
    }

    @Test
    fun newerReleaseOnRecheckIsDownloadedAgain() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))
        viewModel.update()
        advanceUntilIdle()
        val newer = AppRelease(AppVersion(listOf(0, 3)), bodyHtml = "", apk = null)
        check = UpdateCheck.Available(newer, listOf(newer), hasOlderChanges = false)
        viewModel.check()
        advanceUntilIdle()
        download = CompletableDeferred(ApkDownloadResult.Saved(NEWER_APK))

        viewModel.update()
        advanceUntilIdle()

        assertEquals(2, downloads)
        assertEquals(0, apks.discardedAll)
        assertEquals(listOf(APK, NEWER_APK), installer.installed)
    }

    @Test
    fun upToDateOnRecheckDiscardsDownloadedFile() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))
        viewModel.update()
        advanceUntilIdle()
        check = UpdateCheck.UpToDate("0.2")
        viewModel.check()
        advanceUntilIdle()

        viewModel.update()
        advanceUntilIdle()

        assertEquals(UpdateUiState.UpToDate("0.2"), viewModel.state.value)
        assertEquals(1, apks.discardedAll)
        assertEquals(1, downloads)
        assertEquals(listOf(APK), installer.installed)
    }

    @Test
    fun leavingTheScreenAfterRecheckDiscards() = runTest {
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(store, viewModelFactory { initializer { newViewModel() } })[UpdateViewModel::class]
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))
        viewModel.update()
        advanceUntilIdle()
        val newer = AppRelease(AppVersion(listOf(0, 3)), bodyHtml = "", apk = null)
        check = UpdateCheck.Available(newer, listOf(newer), hasOlderChanges = false)
        viewModel.check()
        advanceUntilIdle()
        assertEquals(0, apks.discardedAll)

        store.clear()
        advanceUntilIdle()

        assertEquals(1, apks.discardedAll)
    }

    @Test
    fun sameVersionWithAnotherChecksumIsDownloadedAgain() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))
        viewModel.update()
        advanceUntilIdle()
        val republished = release.copy(apk = ReleaseApk("https://example.com/a.apk", sizeBytes = 1L, sha256 = "f".repeat(64)))
        check = UpdateCheck.Available(republished, listOf(republished), hasOlderChanges = false)
        viewModel.check()
        advanceUntilIdle()

        viewModel.update()
        advanceUntilIdle()

        assertEquals(2, downloads)
    }

    @Test
    fun leavingTheScreenDiscardsDownloadedFile() = runTest {
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(store, viewModelFactory { initializer { newViewModel() } })[UpdateViewModel::class]
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))
        viewModel.update()
        advanceUntilIdle()

        store.clear()
        advanceUntilIdle()

        assertEquals(1, apks.discardedAll)
    }

    @Test
    fun failedDownloadIsShownAndRetryDownloadsAgain() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Failed("нет сети"))

        viewModel.update()
        advanceUntilIdle()
        assertEquals(UpdateStep.Failed("нет сети"), viewModel.step)
        download = CompletableDeferred(ApkDownloadResult.Failed("снова нет"))
        viewModel.update()
        advanceUntilIdle()

        assertEquals(2, downloads)
        assertEquals(UpdateStep.Failed("снова нет"), viewModel.step)
    }

    @Test
    fun finalDownloadFailureIsNotRetryable() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Failed("нет APK", retryable = false))

        viewModel.update()
        advanceUntilIdle()

        assertEquals(UpdateStep.Failed("нет APK", retryable = false), viewModel.step)
    }

    @Test
    fun withoutPermissionAsksAndInstallsAfterReturn() = runTest {
        installer.allowed = false
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))

        viewModel.update()
        advanceUntilIdle()
        assertEquals(UpdateStep.NeedsPermission, viewModel.step)
        viewModel.onResume()
        assertEquals(UpdateStep.NeedsPermission, viewModel.step)
        installer.allowed = true
        viewModel.onResume()

        assertEquals(UpdateStep.Installing, viewModel.step)
        assertEquals(listOf(APK), installer.installed)
        assertEquals(listOf(UsageEvent.UPDATE_DOWNLOAD, UsageEvent.UPDATE_INSTALL), usage.events)
    }

    @Test
    fun installAgainUsesDownloadedFile() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))

        viewModel.update()
        advanceUntilIdle()
        viewModel.update()
        advanceUntilIdle()

        assertEquals(1, downloads)
        assertEquals(listOf(APK, APK), installer.installed)
    }

    @Test
    fun installerThatDoesNotOpenIsReported() = runTest {
        installer.opens = false
        val viewModel = viewModel()
        advanceUntilIdle()
        download.complete(ApkDownloadResult.Saved(APK))

        viewModel.update()
        advanceUntilIdle()

        assertIs<UpdateStep.Failed>(viewModel.step)
        assertEquals(listOf(UsageEvent.UPDATE_DOWNLOAD), usage.events)
    }

    @Test
    fun upToDateAndFailedChecksAreShown() = runTest {
        check = UpdateCheck.UpToDate("0.1")
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(UpdateUiState.UpToDate("0.1"), viewModel.state.value)

        check = UpdateCheck.Failed("Нет связи с GitHub")
        viewModel.check()
        advanceUntilIdle()

        assertEquals(UpdateUiState.Failed("Нет связи с GitHub"), viewModel.state.value)
    }

    private class FakeInstaller : AppInstaller {
        var allowed = true
        var opens = true
        val installed = mutableListOf<Path>()

        override fun canInstall(): Boolean = allowed

        override fun openInstallSettings(): Boolean = true

        override fun install(apk: Path): Boolean {
            installed += apk
            return opens
        }
    }

    private companion object {
        val APK = "/data/updates/DevAtlas-0.2.apk".toPath()
        val NEWER_APK = "/data/updates/DevAtlas-0.3.apk".toPath()
    }
}
