package ru.aokazantsev.devatlas.ui.links

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PdfFiles
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PdfState
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@Inject
class LinksViewModel(
    private val pdfFiles: PdfFiles,
    private val externalApps: ExternalApps,
    private val platform: PlatformBindings,
    private val usage: UsageStatistics,
) : ViewModel() {

    val pdfState: StateFlow<PdfState> = pdfFiles.state

    private val mutableNotice = MutableStateFlow<String?>(null)

    val notice: StateFlow<String?> = mutableNotice.asStateFlow()

    fun openedWeb() {
        usage.hit(UsageEvent.LINK_WEBVIEW)
    }

    fun openPdf(request: PdfRequest) {
        pdfFiles.open(request)
    }

    fun redownloadPdf(request: PdfRequest) {
        pdfFiles.redownload(request)
    }

    fun retryPdf(failed: PdfState.Failed) {
        pdfFiles.dismiss()
        if (failed.redownload) pdfFiles.redownload(failed.request) else pdfFiles.open(failed.request)
    }

    fun cancelPdf() {
        pdfFiles.cancel()
    }

    fun dismissPdf() {
        pdfFiles.dismiss()
    }

    fun openExternal(uri: String) {
        if (!externalApps.openUri(uri)) {
            mutableNotice.value = "На телефоне нет приложения, которое открывает ссылку ${uri.take(SHOWN_URI_LENGTH)}"
        }
    }

    fun copyLink(url: String): Boolean = platform.copyToClipboard(url)

    fun dismissNotice() {
        mutableNotice.value = null
    }

    private companion object {
        const val SHOWN_URI_LENGTH = 200
    }
}
