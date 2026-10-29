package ru.aokazantsev.devatlas.database.data

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object DevAtlasDatabaseConstructor : RoomDatabaseConstructor<DevAtlasDatabase> {
    override fun initialize(): DevAtlasDatabase
}
