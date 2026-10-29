package ru.aokazantsev.devatlas.database.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ViewedPageDao {
    @Query("SELECT COUNT(*) FROM viewed_page")
    suspend fun count(): Int
}
