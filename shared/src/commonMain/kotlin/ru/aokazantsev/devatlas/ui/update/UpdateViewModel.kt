package ru.aokazantsev.devatlas.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.Path
import ru.aokazantsev.devatlas.di.AppLinks
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.journal.domain.AppJournal
import ru.aokazantsev.devatlas.themeformat.page.HtmlPageParser
import ru.aokazantsev.devatlas.update.domain.ApkDownloader
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.update.domain.AppUpdateChecker
import ru.aokazantsev.devatlas.update.domain.model.ApkDownloadResult
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@Inject
class UpdateViewModel(
    private val checker: AppUpdateChecker,
    private val downloader: ApkDownloader,
    private val installer: AppInstaller,
    private val externalApps: ExternalApps,
    private val usage: UsageStatistics,
    private val journal: AppJournal,
) : ViewModel() {

    private val mutableState = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()

    private var downloadJob: Job? = null
    private var downloaded: Path? = null

    init {
        check()
    }

    fun check() {
        if (downloadJob?.isActive == true) return
        mutableState.value = UpdateUiState.Checking
        viewModelScope.launch {
            mutableState.value = when (val check = checker.check()) {
                is UpdateCheck.UpToDate -> UpdateUiState.UpToDate(check.installed)
                is UpdateCheck.Failed -> UpdateUiState.Failed(check.message)
                is UpdateCheck.Available -> UpdateUiState.Available(
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
            }
        }
    }

    fun update() {
        val available = mutableState.value as? UpdateUiState.Available ?: return
        if (downloadJob?.isActive == true) return
        downloaded?.let {
            install(it)
            return
        }
        setStep(UpdateStep.Downloading(receivedBytes = 0L, totalBytes = null))
        downloadJob = viewModelScope.launch {
            val result = downloader.download(available.release) { received, total ->
                if (isActive) setStep(UpdateStep.Downloading(received, total))
            }
            when (result) {
                is ApkDownloadResult.Failed -> {
                    journal.write("update download: failed: ${result.message}")
                    setStep(UpdateStep.Failed(result.message))
                }
                is ApkDownloadResult.Saved -> {
                    journal.write("update download: ${available.release.version} saved, sha256 verified")
                    usage.hit(UsageEvent.UPDATE_DOWNLOAD)
                    downloaded = result.file
                    install(result.file)
                }
            }
        }
    }

    fun cancelDownload() {
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
        val file = downloaded
        if (step == UpdateStep.NeedsPermission && file != null && installer.canInstall()) install(file)
    }

    fun openLink(url: String) {
        externalApps.openUri(url)
    }

    fun openHistory() {
        externalApps.openUri(AppLinks.RELEASES_PAGE_URL)
    }

    private fun install(file: Path) {
        if (!installer.canInstall()) {
            setStep(UpdateStep.NeedsPermission)
            return
        }
        usage.hit(UsageEvent.UPDATE_INSTALL)
        if (installer.install(file)) {
            setStep(UpdateStep.Installing)
        } else {
            setStep(UpdateStep.Failed("Не удалось открыть установщик Android"))
        }
    }

    private fun setStep(step: UpdateStep) {
        mutableState.update { current -> if (current is UpdateUiState.Available) current.copy(step = step) else current }
    }
}
