package ru.aokazantsev.devatlas.files.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.aokazantsev.devatlas.database.data.dao.DownloadedFileDao
import ru.aokazantsev.devatlas.database.data.entity.DownloadedFileEntity
import ru.aokazantsev.devatlas.files.domain.DownloadedFileRepository
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile

@ContributesBinding(AppScope::class)
@Inject
internal class DownloadedFileRepositoryImpl(
    private val dao: DownloadedFileDao,
) : DownloadedFileRepository {

    override fun observeAll(): Flow<List<DownloadedFile>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun find(url: String): DownloadedFile? = dao.find(url)?.toModel()

    override suspend fun save(file: DownloadedFile) {
        dao.upsert(
            DownloadedFileEntity(
                url = file.url,
                contentUri = file.contentUri,
                name = file.name,
                sizeBytes = file.sizeBytes,
                downloadedAtMillis = file.downloadedAtMillis,
            ),
        )
    }

    override suspend fun delete(urls: List<String>) {
        if (urls.isNotEmpty()) dao.delete(urls)
    }

    private fun DownloadedFileEntity.toModel() = DownloadedFile(
        url = url,
        contentUri = contentUri,
        name = name,
        sizeBytes = sizeBytes,
        downloadedAtMillis = downloadedAtMillis,
    )
}
