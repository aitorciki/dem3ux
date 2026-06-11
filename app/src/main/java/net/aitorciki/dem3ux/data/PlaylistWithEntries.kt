package net.aitorciki.dem3ux.data

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistWithEntries(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
    )
    val entries: List<PlaylistEntryEntity>,
)
