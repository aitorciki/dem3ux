package net.aitorciki.dem3ux.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.aitorciki.dem3ux.data.Dem3uxDatabaseProvider
import net.aitorciki.dem3ux.data.PlaylistRepository
import net.aitorciki.dem3ux.data.PlaylistWithEntries

class Dem3uxViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = PlaylistRepository(Dem3uxDatabaseProvider.get(application))
    private val selectedPlaylistId = MutableStateFlow<Long?>(null)
    private val importMessage = MutableStateFlow<String?>(null)

    val uiState =
        combine(repository.observePlaylistsWithEntries(), selectedPlaylistId, importMessage) { playlists, selectedId, message ->
            val sortedPlaylists = playlists.map { playlist -> playlist.withSortedEntries() }
            val selectedPlaylist = sortedPlaylists.firstOrNull { playlist -> playlist.playlist.id == selectedId }

            Dem3uxUiState(
                playlists = sortedPlaylists.map { playlist -> playlist.toSummaryUi() },
                selectedPlaylist = selectedPlaylist?.toDetailUi(),
                importMessage = message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Dem3uxUiState(),
        )

    fun selectPlaylist(playlistId: Long) {
        selectedPlaylistId.value = playlistId
    }

    fun clearSelectedPlaylist() {
        selectedPlaylistId.value = null
    }

    fun selectEntry(
        playlistId: Long,
        entryIndex: Int,
    ) {
        viewModelScope.launch {
            repository.selectEntry(playlistId = playlistId, entryIndex = entryIndex)
        }
    }

    fun importPlaylist(uri: Uri) {
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val application = getApplication<Application>()
                        application.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val content =
                            requireNotNull(application.contentResolver.openInputStream(uri)) { "Could not open playlist" }
                                .bufferedReader()
                                .use { reader -> reader.readText() }

                        repository.recordSeenPlaylist(sourcePath = uri.toString(), content = content)
                    }
                }

            val selection = result.getOrNull()
            if (selection == null) {
                importMessage.value = "Could not import playlist"
            } else {
                selectedPlaylistId.value = selection.playlistId
                importMessage.value = "Playlist added"
            }
        }
    }

    fun clearImportMessage() {
        importMessage.value = null
    }

    private fun PlaylistWithEntries.withSortedEntries(): PlaylistWithEntries =
        copy(entries = entries.sortedBy { entry -> entry.entryIndex })

    private fun PlaylistWithEntries.toSummaryUi(): PlaylistSummaryUi {
        val selectedEntry = entries.firstOrNull { entry -> entry.entryIndex == playlist.selectedEntryIndex } ?: entries.firstOrNull()

        return PlaylistSummaryUi(
            id = playlist.id,
            displayName = playlist.displayName,
            sourcePath = playlist.sourcePath,
            selectedEntryName = selectedEntry?.displayName ?: "No entries",
            lastSeenLabel = "Last accessed ${playlist.lastSeenAt.toRelativeLabel()}",
        )
    }

    private fun PlaylistWithEntries.toDetailUi(): PlaylistDetailUi {
        val selectedIndex = playlist.selectedEntryIndex ?: entries.firstOrNull()?.entryIndex

        return PlaylistDetailUi(
            id = playlist.id,
            displayName = playlist.displayName,
            sourcePath = playlist.sourcePath,
            sourcePathLabel = SourcePathLabel.format(playlist.sourcePath),
            entries =
                entries.map { entry ->
                    PlaylistEntryUi(
                        index = entry.entryIndex,
                        displayName = entry.displayName,
                        rawLine = entry.rawLine,
                        resolvedPath = entry.resolvedPath,
                        selected = entry.entryIndex == selectedIndex,
                    )
                },
        )
    }
}

private fun Long.toRelativeLabel(): String {
    val elapsedMillis = (System.currentTimeMillis() - this).coerceAtLeast(0)
    val elapsedMinutes = elapsedMillis / 60_000
    val elapsedHours = elapsedMinutes / 60
    val elapsedDays = elapsedHours / 24

    return when {
        elapsedMinutes < 1 -> "just now"
        elapsedMinutes == 1L -> "1 minute ago"
        elapsedMinutes < 60 -> "$elapsedMinutes minutes ago"
        elapsedHours == 1L -> "1 hour ago"
        elapsedHours < 24 -> "$elapsedHours hours ago"
        elapsedDays == 1L -> "yesterday"
        else -> "$elapsedDays days ago"
    }
}
