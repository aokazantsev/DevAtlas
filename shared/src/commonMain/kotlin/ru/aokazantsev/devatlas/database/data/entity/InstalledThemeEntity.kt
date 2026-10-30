package ru.aokazantsev.devatlas.database.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_theme")
data class InstalledThemeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    val etag: String?,
    @ColumnInfo(name = "last_modified") val lastModified: String?,
    val sha256: String,
    @ColumnInfo(name = "checked_at") val checkedAt: Long,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long,
)
