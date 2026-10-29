package ru.aokazantsev.devatlas.database.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity

@Database(entities = [ViewedPageEntity::class], version = 1)
@ConstructedBy(DevAtlasDatabaseConstructor::class)
abstract class DevAtlasDatabase : RoomDatabase() {
    abstract fun viewedPageDao(): ViewedPageDao
}
