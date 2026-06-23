package net.aitorciki.dem3ux.data

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "playlists", columnName = "pathKind"),
)
class DropPathKindSpec : AutoMigrationSpec
