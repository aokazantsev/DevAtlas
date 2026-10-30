package ru.aokazantsev.devatlas.database.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.aokazantsev.devatlas.database.data.entity.ReadingPointEntity

@Dao
interface ReadingPointDao {
    @Upsert
    suspend fun upsert(entity: ReadingPointEntity)

    @Query("SELECT * FROM reading_point WHERE theme_id = :themeId")
    suspend fun find(themeId: String): ReadingPointEntity?

    @Query("DELETE FROM reading_point WHERE theme_id = :themeId")
    suspend fun deleteForTheme(themeId: String)

    @Query(
        "DELETE FROM reading_point WHERE theme_id = :themeId AND (" +
            "(kind = 'page' AND node_id NOT IN (:pageIds)) OR (kind = 'section' AND node_id NOT IN (:sectionIds)))",
    )
    suspend fun deleteMissing(themeId: String, pageIds: List<String>, sectionIds: List<String>)
}
