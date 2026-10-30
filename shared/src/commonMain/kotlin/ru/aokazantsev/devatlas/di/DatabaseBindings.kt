package ru.aokazantsev.devatlas.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase
import ru.aokazantsev.devatlas.database.data.dao.BookmarkDao
import ru.aokazantsev.devatlas.database.data.dao.InstalledThemeDao
import ru.aokazantsev.devatlas.database.data.dao.ReadingPointDao
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao

@BindingContainer
internal object DatabaseBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(platform: PlatformBindings, appInfo: AppInfo): DevAtlasDatabase {
        val builder = platform.databaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
        if (appInfo.isDebug) builder.fallbackToDestructiveMigration(dropAllTables = true)
        return builder.build()
    }

    @Provides
    fun provideViewedPageDao(database: DevAtlasDatabase): ViewedPageDao = database.viewedPageDao()

    @Provides
    fun provideInstalledThemeDao(database: DevAtlasDatabase): InstalledThemeDao = database.installedThemeDao()

    @Provides
    fun provideBookmarkDao(database: DevAtlasDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideReadingPointDao(database: DevAtlasDatabase): ReadingPointDao = database.readingPointDao()
}
