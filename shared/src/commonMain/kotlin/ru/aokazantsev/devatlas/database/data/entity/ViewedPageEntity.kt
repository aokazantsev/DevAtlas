package ru.aokazantsev.devatlas.database.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "viewed_page", primaryKeys = ["theme_id", "page_id"])
data class ViewedPageEntity(
    @ColumnInfo(name = "theme_id") val themeId: String,
    @ColumnInfo(name = "page_id") val pageId: String,
)
