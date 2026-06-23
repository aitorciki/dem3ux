package net.aitorciki.dem3ux.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistEntryEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = DropPathKindSpec::class)],
)
abstract class Dem3uxDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
