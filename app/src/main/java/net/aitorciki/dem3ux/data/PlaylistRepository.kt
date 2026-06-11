package net.aitorciki.dem3ux.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import net.aitorciki.dem3ux.bridge.PlaylistEntryResolver
import net.aitorciki.dem3ux.m3u.M3uEntry
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class PlaylistLaunchSelection(
    val playlistId: Long,
    val selectedEntryPath: String,
)

class PlaylistRepository(
    private val database: Dem3uxDatabase,
) {
    private val playlistDao = database.playlistDao()

    fun observePlaylistsWithEntries(): Flow<List<PlaylistWithEntries>> = playlistDao.observePlaylistsWithEntries()

    fun observePlaylistWithEntries(playlistId: Long): Flow<PlaylistWithEntries?> = playlistDao.observePlaylistWithEntries(playlistId)

    suspend fun recordSeenPlaylist(
        sourcePath: String,
        content: String,
        now: Long = System.currentTimeMillis(),
    ): PlaylistLaunchSelection? {
        val parsedEntries = PlaylistEntryResolver.resolveEntries(sourcePath = sourcePath, content = content)
        if (parsedEntries.isEmpty()) {
            return null
        }

        return database.withTransaction {
            val existing = playlistDao.getPlaylistBySourcePath(sourcePath)
            val selectedIndex = PlaylistSelectionPolicy.selectedIndex(existing?.selectedEntryIndex, parsedEntries)
            val playlistId = upsertPlaylist(sourcePath = sourcePath, existing = existing, selectedIndex = selectedIndex, now = now)

            playlistDao.deleteEntries(playlistId)
            playlistDao.insertEntries(parsedEntries.map { entry -> entry.toEntity(playlistId) })

            PlaylistLaunchSelection(
                playlistId = playlistId,
                selectedEntryPath = parsedEntries.first { entry -> entry.index == selectedIndex }.resolvedPath,
            )
        }
    }

    suspend fun selectEntry(
        playlistId: Long,
        entryIndex: Int,
    ) {
        playlistDao.updateSelectedEntry(playlistId = playlistId, entryIndex = entryIndex)
    }

    private suspend fun upsertPlaylist(
        sourcePath: String,
        existing: PlaylistEntity?,
        selectedIndex: Int?,
        now: Long,
    ): Long {
        val playlist =
            PlaylistEntity(
                id = existing?.id ?: 0,
                sourcePath = sourcePath,
                displayName = sourcePath.toDisplayName(),
                pathKind = sourcePath.toPathKind(),
                selectedEntryIndex = selectedIndex,
                firstSeenAt = existing?.firstSeenAt ?: now,
                lastSeenAt = now,
                lastParsedAt = now,
            )

        if (existing == null) {
            return playlistDao.insertPlaylist(playlist)
        }

        playlistDao.updatePlaylist(playlist)
        return existing.id
    }

    private fun M3uEntry.toEntity(playlistId: Long): PlaylistEntryEntity =
        PlaylistEntryEntity(
            playlistId = playlistId,
            entryIndex = index,
            rawLine = rawLine,
            resolvedPath = resolvedPath,
            displayName = rawLine.toDisplayName(),
        )
}

fun String.toPathKind(): String =
    when {
        startsWith("content://") -> "safUri"
        startsWith("file://") -> "fileUri"
        startsWith("/") -> "filesystem"
        else -> "unknown"
    }

fun String.toDisplayName(): String {
    val rawPath =
        runCatching {
            when {
                startsWith("content://") -> URLDecoder.decode(URI(this).path.substringAfterLast('/'), StandardCharsets.UTF_8.name())
                startsWith("file://") -> URI(this).path
                else -> this
            }
        }.getOrDefault(this)

    return rawPath
        .substringAfterLast('/')
        .substringAfterLast(':')
        .substringBeforeLast('.', missingDelimiterValue = rawPath.substringAfterLast('/').substringAfterLast(':'))
        .ifBlank { this }
}
