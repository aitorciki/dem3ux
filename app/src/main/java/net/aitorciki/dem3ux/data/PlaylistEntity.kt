package net.aitorciki.dem3ux.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["sourcePath"], unique = true)],
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourcePath: String,
    val displayName: String,
    val pathKind: String,
    val selectedEntryIndex: Int?,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val lastParsedAt: Long,
)
