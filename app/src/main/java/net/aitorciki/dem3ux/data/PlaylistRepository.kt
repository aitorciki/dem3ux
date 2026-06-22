package net.aitorciki.dem3ux.data

import kotlinx.coroutines.flow.Flow

data class PlaylistLaunchSelection(
    val playlistId: Long,
    val selectedEntryPath: String,
)

interface PlaylistRepository {
    fun observePlaylistsWithEntries(): Flow<List<PlaylistWithEntries>>

    suspend fun recordSeenPlaylist(
        sourcePath: String,
        content: String,
        now: Long = System.currentTimeMillis(),
    ): PlaylistLaunchSelection?

    suspend fun selectEntry(
        playlistId: Long,
        entryIndex: Int,
    )

    suspend fun deletePlaylist(playlistId: Long)
}
