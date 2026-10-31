package ru.aokazantsev.devatlas.database.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.database.data.entity.DownloadedFileEntity

@Dao
interface DownloadedFileDao {
    @Upsert
    suspend fun upsert(entity: DownloadedFileEntity)

    @Query("SELECT * FROM downloaded_file WHERE url = :url")
    suspend fun find(url: String): DownloadedFileEntity?

    @Query("SELECT * FROM downloaded_file ORDER BY downloaded_at DESC, name")
    fun observeAll(): Flow<List<DownloadedFileEntity>>

    @Query("DELETE FROM downloaded_file WHERE url IN (:urls)")
    suspend fun delete(urls: List<String>)
}
