package ru.aokazantsev.devatlas.database.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.aokazantsev.devatlas.database.data.entity.InstalledThemeEntity

@Dao
interface InstalledThemeDao {
    @Query("SELECT * FROM installed_theme ORDER BY last_used_at DESC, title")
    fun observeAll(): Flow<List<InstalledThemeEntity>>

    @Query("SELECT * FROM installed_theme ORDER BY last_used_at DESC, title")
    suspend fun all(): List<InstalledThemeEntity>

    @Query("SELECT * FROM installed_theme WHERE id = :themeId")
    suspend fun find(themeId: String): InstalledThemeEntity?

    @Upsert
    suspend fun upsert(entity: InstalledThemeEntity)

    @Query("DELETE FROM installed_theme WHERE id = :themeId")
    suspend fun delete(themeId: String)

    @Query("UPDATE installed_theme SET last_used_at = :now WHERE id = :themeId")
    suspend fun touch(themeId: String, now: Long)

    @Query("UPDATE installed_theme SET checked_at = :now WHERE id = :themeId")
    suspend fun markChecked(themeId: String, now: Long)
}
