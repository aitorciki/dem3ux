package net.aitorciki.dem3ux.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY lastSeenAt DESC")
    fun observePlaylistsWithEntries(): Flow<List<PlaylistWithEntries>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylistWithEntries(playlistId: Long): Flow<PlaylistWithEntries?>

    @Query("SELECT * FROM playlists WHERE sourcePath = :sourcePath LIMIT 1")
    suspend fun getPlaylistBySourcePath(sourcePath: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun deleteEntries(playlistId: Long)

    @Insert
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    @Query("UPDATE playlists SET selectedEntryIndex = :entryIndex WHERE id = :playlistId")
    suspend fun updateSelectedEntry(
        playlistId: Long,
        entryIndex: Int,
    )
}
