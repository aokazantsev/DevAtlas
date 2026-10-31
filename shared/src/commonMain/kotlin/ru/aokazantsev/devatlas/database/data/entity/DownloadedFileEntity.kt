package ru.aokazantsev.devatlas.database.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_file")
data class DownloadedFileEntity(
    @PrimaryKey val url: String,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    val name: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "downloaded_at") val downloadedAtMillis: Long,
)
