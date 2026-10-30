package ru.aokazantsev.devatlas.database.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark", indices = [Index("theme_id")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "theme_id") val themeId: String,
    val kind: String,
    @ColumnInfo(name = "node_id") val nodeId: String,
    val anchor: Int,
    val title: String,
    val position: Int,
)
