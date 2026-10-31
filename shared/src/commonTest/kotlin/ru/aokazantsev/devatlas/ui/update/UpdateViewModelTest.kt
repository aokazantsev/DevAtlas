package ru.aokazantsev.devatlas.ui.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path
import okio.Path.Companion.toPath
import ru.aokazantsev.devatlas.files.FakeExternalApps
import ru.aokazantsev.devatlas.testing.FakeJournal
import ru.aokazantsev.devatlas.testing.FakeUsageStatistics
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.update.domain.ApkDownloader
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.update.domain.AppUpdateChecker
import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
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

    private val checker = object : AppUpdateChecker {
        override suspend fun check(): UpdateCheck = check

        override suspend fun checkIfDue(): UpdateCheck.Available? = null

        override suspend fun markAnnounced(check: UpdateCheck.Available) = Unit
    }

    private val downloader = object : ApkDownloader {
        override suspend fun download(release: AppRelease, onProgress: (Long, Long?) -> Unit): ApkDownloadResult {
            downloads++
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

    private fun viewModel() = scopes.track(
        UpdateViewModel(checker, downloader, installer, FakeExternalApps(), usage, FakeJournal()),
    )

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
    }
}
