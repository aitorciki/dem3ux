package net.aitorciki.dem3ux.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class Dem3uxDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
