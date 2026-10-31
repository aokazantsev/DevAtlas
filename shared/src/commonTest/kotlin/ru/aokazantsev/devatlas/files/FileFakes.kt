package ru.aokazantsev.devatlas.files

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Buffer
import okio.IOException
import okio.Sink
import ru.aokazantsev.devatlas.files.domain.DownloadedFileRepository
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PdfDownloader
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile
import ru.aokazantsev.devatlas.files.domain.model.PdfDownloadResult
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.files.domain.model.PendingPublicFile

class FakePublicFile(var name: String, val content: Buffer = Buffer(), var pending: Boolean = true)

class FakePublicFiles : PublicFiles {
    override val folderName: String = "DevAtlas"

    val files = linkedMapOf<String, FakePublicFile>()
    val unreadable = mutableSetOf<String>()
    var failCreate = false
    var failWrite = false
    private var counter = 0

    fun published(): Map<String, FakePublicFile> = files.filterValues { !it.pending }

    fun add(name: String): String {
        val uri = "content://media/downloads/${++counter}"
        files[uri] = FakePublicFile(name, pending = false)
        return uri
    }

    override fun create(name: String, mimeType: String): PendingPublicFile {
        if (failCreate) throw IOException("нет места")
        val uri = "content://media/downloads/${++counter}"
        val file = FakePublicFile(name)
        files[uri] = file
        return object : PendingPublicFile {
            override val uri: String = uri

            override fun sink(): Sink {
                if (failWrite) throw IOException("диск полон")
                return file.content
            }

            override fun publish(): String {
                val taken = files.values.filter { !it.pending }.map { it.name }.toSet()
                val base = name.removeSuffix(".pdf")
                file.name = generateSequence(0) { it + 1 }
                    .map { if (it == 0) name else "$base ($it).pdf" }
                    .first { it !in taken }
                file.pending = false
                return file.name
            }

            override fun discard() {
                files.remove(uri)
            }
        }
    }

    override fun isReadable(uri: String): Boolean = uri in files && uri !in unreadable

    override fun delete(uri: String): Boolean = files.remove(uri) != null
}

class FakeExternalApps(var opens: Boolean = true, var shares: Boolean = true) : ExternalApps {
    val openedUris = mutableListOf<String>()
    val openedFiles = mutableListOf<String>()
    val sharedFiles = mutableListOf<String>()

    override fun openUri(uri: String): Boolean {
        openedUris += uri
        return opens
    }

    override fun openFile(uri: String, mimeType: String): Boolean {
        openedFiles += uri
        return opens
    }

    override fun shareFile(uri: String, mimeType: String): Boolean {
        sharedFiles += uri
        return shares
    }
}

class FakeDownloadedFileRepository : DownloadedFileRepository {
    val files = MutableStateFlow<List<DownloadedFile>>(emptyList())

    override fun observeAll(): Flow<List<DownloadedFile>> = files

    override suspend fun find(url: String): DownloadedFile? = files.value.firstOrNull { it.url == url }

    override suspend fun save(file: DownloadedFile) {
        files.value = files.value.filter { it.url != file.url } + file
    }

    override suspend fun delete(urls: List<String>) {
        files.value = files.value.filter { it.url !in urls }
    }
}

class GatedPdfDownloader : PdfDownloader {
    val gate = CompletableDeferred<PdfDownloadResult>()
    val requests = mutableListOf<PdfRequest>()
    var progress: ((Long, Long?) -> Unit)? = null

    override suspend fun download(
        request: PdfRequest,
        replacing: String?,
        onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): PdfDownloadResult {
        requests += request
        progress = onProgress
        return gate.await()
    }
}

class ScriptedPdfDownloader(private val publicFiles: FakePublicFiles) : PdfDownloader {
    var failure: String? = null
    val requests = mutableListOf<Pair<PdfRequest, String?>>()

    override suspend fun download(
        request: PdfRequest,
        replacing: String?,
        onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): PdfDownloadResult {
        requests += request to replacing
        failure?.let { return PdfDownloadResult.Failed(it) }
        onProgress(SIZE, SIZE)
        replacing?.let(publicFiles::delete)
        val uri = publicFiles.add(request.fileName ?: "file.pdf")
        return PdfDownloadResult.Saved(DownloadedFile(request.url, uri, publicFiles.files.getValue(uri).name, SIZE, 0L))
    }

    private companion object {
        const val SIZE = 10L
    }
}
