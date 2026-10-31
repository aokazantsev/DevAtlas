package ru.aokazantsev.devatlas.database.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.aokazantsev.devatlas.database.data.dao.BookmarkDao
import ru.aokazantsev.devatlas.database.data.dao.DownloadedFileDao
import ru.aokazantsev.devatlas.database.data.dao.InstalledThemeDao
import ru.aokazantsev.devatlas.database.data.dao.ReadingPointDao
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao
import ru.aokazantsev.devatlas.database.data.entity.BookmarkEntity
import ru.aokazantsev.devatlas.database.data.entity.DownloadedFileEntity
import ru.aokazantsev.devatlas.database.data.entity.InstalledThemeEntity
import ru.aokazantsev.devatlas.database.data.entity.ReadingPointEntity
import ru.aokazantsev.devatlas.database.data.entity.ViewedPageEntity

@Database(
    entities = [
        ViewedPageEntity::class,
        InstalledThemeEntity::class,
        BookmarkEntity::class,
        ReadingPointEntity::class,
        DownloadedFileEntity::class,
    ],
    version = 4,
)
@ConstructedBy(DevAtlasDatabaseConstructor::class)
abstract class DevAtlasDatabase : RoomDatabase() {
    abstract fun viewedPageDao(): ViewedPageDao

    abstract fun installedThemeDao(): InstalledThemeDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun readingPointDao(): ReadingPointDao

    abstract fun downloadedFileDao(): DownloadedFileDao
}
