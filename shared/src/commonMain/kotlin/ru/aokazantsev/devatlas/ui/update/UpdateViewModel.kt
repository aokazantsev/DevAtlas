package ru.aokazantsev.devatlas.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okio.Path
import ru.aokazantsev.devatlas.di.AppLinks
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.themeformat.page.HtmlPageParser
import ru.aokazantsev.devatlas.update.domain.ApkDownloader
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.update.domain.AppUpdateChecker
import ru.aokazantsev.devatlas.update.domain.UpdateApkStorage
import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent
import kotlin.concurrent.Volatile

@Inject
@Suppress("LongParameterList")
class UpdateViewModel(
    private val checker: AppUpdateChecker,
    private val downloader: ApkDownloader,
    private val installer: AppInstaller,
    private val externalApps: ExternalApps,
    private val usage: UsageStatistics,
    private val journal: AppJournal,
    private val apks: UpdateApkStorage,
    private val scope: CoroutineScope,
) : ViewModel() {

    private val mutableState = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()

    private var downloadJob: Job? = null
    private var downloaded: DownloadedUpdate? = null

    @Volatile
    private var stepOwner: Job? = null

    init {
        check()
    }

    fun check() {
        if (downloadJob?.isActive == true) return
        mutableState.value = UpdateUiState.Checking
        viewModelScope.launch {
            mutableState.value = when (val check = checker.check()) {
                is UpdateCheck.UpToDate -> {
                    discardDownload()
                    UpdateUiState.UpToDate(check.installed)
                }
                is UpdateCheck.Failed -> UpdateUiState.Failed(check.message)
                is UpdateCheck.Available -> {
                    reusableDownload(check.latest)
                    available(check)
                }
            }
        }
    }

    fun update() {
        val available = mutableState.value as? UpdateUiState.Available ?: return
        if (downloadJob?.isActive == true) return
        reusableDownload(available.release)?.let {
            install(it.file)
            return
        }
        setStep(UpdateStep.Downloading(receivedBytes = 0L, totalBytes = null))
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext.job
            val result = downloader.download(available.release) { received, total ->
                setStep(UpdateStep.Downloading(received, total), owner = self)
            }
            when (result) {
                is ApkDownloadResult.Failed -> {
                    journal.write("update download: failed: ${result.message}")
                    setStep(UpdateStep.Failed(result.message, result.retryable), owner = self)
                }
                is ApkDownloadResult.Saved -> {
                    journal.write("update download: ${available.release.version} saved, sha256 verified")
                    usage.hit(UsageEvent.UPDATE_DOWNLOAD)
                    val ready = DownloadedUpdate(result.file, available.release)
                    downloaded = ready
                    install(ready.file, owner = self)
                }
            }
        }
        stepOwner = job
        downloadJob = job
        job.start()
    }

    fun cancelDownload() {
        stepOwner = null
        downloadJob?.cancel()
        setStep(UpdateStep.Idle)
    }

    fun allowInstall() {
        if (!installer.openInstallSettings()) {
            setStep(UpdateStep.Failed("Не удалось открыть настройки: разрешите установку приложений из DevAtlas в настройках Android"))
        }
    }

    fun onResume() {
        val step = (mutableState.value as? UpdateUiState.Available)?.step
        val file = downloaded?.file
        if (step == UpdateStep.NeedsPermission && file != null && installer.canInstall()) install(file)
    }

    fun openLink(url: String) {
        externalApps.openUri(url)
    }

    fun openHistory() {
        externalApps.openUri(AppLinks.RELEASES_PAGE_URL)
    }

    override fun onCleared() {
        discardDownload()
        super.onCleared()
    }

    private fun available(check: UpdateCheck.Available) = UpdateUiState.Available(
        release = check.latest,
        notes = check.changes.map { release ->
            ReleaseNotes(
                version = release.version.toString(),
                blocks = HtmlPageParser.parse("v${release.version}", release.bodyHtml).document.blocks,
            )
        },
        hasOlderNotes = check.hasOlderChanges,
        offlineNote = check.offlineNote,
    )

    private fun reusableDownload(release: AppRelease): DownloadedUpdate? {
        val ready = downloaded ?: return null
        if (ready.matches(release)) return ready
        downloaded = null
        return null
    }

    private fun discardDownload() {
        downloaded = null
        scope.launch { apks.discardAll() }
    }

    private fun install(file: Path, owner: Job? = null) {
        if (!installer.canInstall()) {
            setStep(UpdateStep.NeedsPermission, owner)
            return
        }
        if (installer.install(file)) {
            journal.write("update install: installer opened")
            usage.hit(UsageEvent.UPDATE_INSTALL)
            setStep(UpdateStep.Installing, owner)
        } else {
            setStep(UpdateStep.Failed("Не удалось открыть установщик Android"), owner)
        }
    }

    private fun setStep(step: UpdateStep, owner: Job? = null) {
        mutableState.update { current ->
            val allowed = owner == null || stepOwner === owner
            if (current is UpdateUiState.Available && allowed) current.copy(step = step) else current
        }
    }
}
