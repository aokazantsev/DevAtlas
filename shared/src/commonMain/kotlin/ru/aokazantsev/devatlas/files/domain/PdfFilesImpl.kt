package ru.aokazantsev.devatlas.files.domain

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile
import ru.aokazantsev.devatlas.files.domain.model.PdfDownloadResult
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PdfState
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class PdfFilesImpl(
    private val downloader: PdfDownloader,
    private val repository: DownloadedFileRepository,
    private val publicFiles: PublicFiles,
    private val externalApps: ExternalApps,
    private val scope: CoroutineScope,
    private val usage: UsageStatistics,
) : PdfFiles {

    private val owned = MutableStateFlow(OwnedPdfState(owner = null, state = PdfState.Idle))

    override val state: StateFlow<PdfState> = owned.map { it.state }.stateIn(scope, SharingStarted.Eagerly, PdfState.Idle)

    override fun open(request: PdfRequest) {
        start(request, redownload = false)
    }

    override fun redownload(request: PdfRequest) {
        start(request, redownload = true)
    }

    override fun openSaved(file: DownloadedFile) {
        val next = if (externalApps.openFile(file.contentUri, PDF_TYPE)) PdfState.Idle else PdfState.NoViewer(file.name)
        owned.update { it.copy(state = next) }
    }

    override fun cancel() {
        val owner = owned.value.owner
        owned.update { OwnedPdfState(owner = null, state = PdfState.Idle) }
        owner?.cancel()
    }

    override fun dismiss() {
        owned.update { current -> if (current.state is PdfState.Downloading) current else current.copy(state = PdfState.Idle) }
    }

    private fun start(request: PdfRequest, redownload: Boolean) {
        if (owned.value.owner?.isActive == true) return
        val started = scope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext.job
            val update = { state: PdfState ->
                owned.update { current -> if (current.owner === self) current.copy(state = state) else current }
            }
            val known = try {
                repository.find(request.url)
            } catch (_: SQLiteException) {
                null
            }
            if (!redownload && known != null && publicFiles.isReadable(known.contentUri)) {
                usage.hit(UsageEvent.PDF_OPEN)
                update(show(known))
                return@launch
            }
            update(PdfState.Downloading(receivedBytes = 0L, totalBytes = null))
            val result = downloader.download(request, replacing = known?.contentUri) { received, total ->
                update(PdfState.Downloading(received, total))
            }
            when (result) {
                is PdfDownloadResult.Failed -> update(PdfState.Failed(result.message, request, redownload))
                is PdfDownloadResult.Saved -> {
                    withContext(NonCancellable) {
                        try {
                            repository.save(result.file)
                        } catch (_: SQLiteException) {
                        }
                    }
                    ensureActive()
                    usage.hit(UsageEvent.PDF_DOWNLOAD)
                    update(show(result.file))
                }
            }
        }
        owned.update { it.copy(owner = started) }
        started.start()
    }

    private fun show(file: DownloadedFile): PdfState = when {
        !externalApps.isAppInForeground() -> PdfState.Ready(file)
        externalApps.openFile(file.contentUri, PDF_TYPE) -> PdfState.Idle
        else -> PdfState.NoViewer(file.name)
    }

    private companion object {
        const val PDF_TYPE = "application/pdf"
    }
}
