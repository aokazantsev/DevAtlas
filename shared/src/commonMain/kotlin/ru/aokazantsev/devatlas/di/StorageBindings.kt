package ru.aokazantsev.devatlas.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase
import ru.aokazantsev.devatlas.database.data.dao.ViewedPageDao

@BindingContainer
internal object StorageBindings {

    private const val SETTINGS_FILE = "settings.preferences_pb"

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsDataStore(platform: PlatformBindings): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { platform.dataDirectory / SETTINGS_FILE },
        )

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
}
