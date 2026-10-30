package ru.aokazantsev.devatlas.database.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity

@Dao
interface ViewedPageDao {
    @Upsert
    suspend fun upsert(entity: ViewedPageEntity)

    @Query("SELECT COUNT(*) FROM viewed_page")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM viewed_page WHERE theme_id = :themeId")
    suspend fun countForTheme(themeId: String): Int

    @Query("DELETE FROM viewed_page WHERE theme_id = :themeId")
    suspend fun deleteForTheme(themeId: String)

    @Query("DELETE FROM viewed_page WHERE theme_id = :themeId AND page_id NOT IN (:keepPageIds)")
    suspend fun deleteExcept(themeId: String, keepPageIds: List<String>)
}
