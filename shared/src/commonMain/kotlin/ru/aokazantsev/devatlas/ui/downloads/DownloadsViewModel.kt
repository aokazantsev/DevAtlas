package ru.aokazantsev.devatlas.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.files.domain.DownloadedFileRepository
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PdfFiles
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.text.CivilDate
import ru.aokazantsev.devatlas.ui.format.formatSize

@Inject
class DownloadsViewModel(
    private val repository: DownloadedFileRepository,
    private val publicFiles: PublicFiles,
    private val externalApps: ExternalApps,
    private val pdfFiles: PdfFiles,
    private val platform: PlatformBindings,
) : ViewModel() {

    val state: StateFlow<DownloadsState> = repository.observeAll()
        .map { files -> DownloadsState.Ready(files.map(::toRow)) as DownloadsState }
        .catch { emit(DownloadsState.Ready(emptyList())) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DownloadsState.Loading)

    private val mutableNotice = MutableStateFlow<String?>(null)

    val notice: StateFlow<String?> = mutableNotice.asStateFlow()

    fun open(row: DownloadRow) {
        pdfFiles.open(PdfRequest(url = row.file.url, fileName = row.file.name))
    }

    fun share(row: DownloadRow) {
        mutableNotice.value = when {
            !row.available -> "Файла «${row.file.name}» нет в Загрузках: нажмите на строку, чтобы скачать его заново"
            !externalApps.shareFile(row.file.contentUri, PDF_TYPE) -> "На телефоне нет приложения, чтобы поделиться файлом"
            else -> null
        }
    }

    fun delete(row: DownloadRow) {
        remove(listOf(row.file))
    }

    fun deleteAll() {
        val rows = (state.value as? DownloadsState.Ready)?.rows ?: return
        remove(rows.map { it.file })
    }

    fun dismissNotice() {
        mutableNotice.value = null
    }

    private fun remove(files: List<DownloadedFile>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                files.forEach { publicFiles.delete(it.contentUri) }
                try {
                    repository.delete(files.map { it.url })
                } catch (_: SQLiteException) {
                    mutableNotice.value = "Не удалось обновить список файлов, попробуйте ещё раз"
                }
            }
        }
    }

    private fun toRow(file: DownloadedFile): DownloadRow {
        val available = publicFiles.isReadable(file.contentUri)
        val localMillis = file.downloadedAtMillis + platform.timeZoneOffsetMillis(file.downloadedAtMillis)
        val details = if (available) {
            "${formatSize(file.sizeBytes)} · ${CivilDate.ofEpochMillis(localMillis).russian()}"
        } else {
            "Файла нет в Загрузках — нажмите, чтобы скачать заново"
        }
        return DownloadRow(file = file, available = available, details = details)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val PDF_TYPE = "application/pdf"
    }
}
