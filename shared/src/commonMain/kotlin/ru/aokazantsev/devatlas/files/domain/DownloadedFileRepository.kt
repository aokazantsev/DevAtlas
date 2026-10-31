package ru.aokazantsev.devatlas.files.domain

import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.files.domain.model.DownloadedFile

interface DownloadedFileRepository {
    fun observeAll(): Flow<List<DownloadedFile>>

    suspend fun find(url: String): DownloadedFile?

    suspend fun save(file: DownloadedFile)

    suspend fun delete(urls: List<String>)
}
