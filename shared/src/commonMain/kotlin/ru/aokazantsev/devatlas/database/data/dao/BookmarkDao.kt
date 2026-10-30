package ru.aokazantsev.devatlas.database.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.database.data.entity.BookmarkEntity

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark WHERE theme_id = :themeId ORDER BY position, id")
    fun observe(themeId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark WHERE theme_id = :themeId ORDER BY position, id")
    suspend fun all(themeId: String): List<BookmarkEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM bookmark WHERE theme_id = :themeId")
    suspend fun nextPosition(themeId: String): Int

    @Insert
    suspend fun insert(entity: BookmarkEntity)

    @Update
    suspend fun update(entities: List<BookmarkEntity>)

    @Query("UPDATE bookmark SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM bookmark WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM bookmark WHERE theme_id = :themeId")
    suspend fun countForTheme(themeId: String): Int

    @Query("DELETE FROM bookmark WHERE theme_id = :themeId")
    suspend fun deleteForTheme(themeId: String)

    @Query(
        "DELETE FROM bookmark WHERE theme_id = :themeId AND (" +
            "(kind = 'page' AND node_id NOT IN (:pageIds)) OR (kind = 'section' AND node_id NOT IN (:sectionIds)))",
    )
    suspend fun deleteMissing(themeId: String, pageIds: List<String>, sectionIds: List<String>)
}
