package ru.aokazantsev.devatlas.database.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_point")
data class ReadingPointEntity(
    @PrimaryKey @ColumnInfo(name = "theme_id") val themeId: String,
    val kind: String,
    @ColumnInfo(name = "node_id") val nodeId: String,
    val anchor: Int,
)
