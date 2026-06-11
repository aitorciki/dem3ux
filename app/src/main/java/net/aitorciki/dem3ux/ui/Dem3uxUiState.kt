package net.aitorciki.dem3ux.ui

data class Dem3uxUiState(
    val playlists: List<PlaylistSummaryUi> = emptyList(),
    val selectedPlaylist: PlaylistDetailUi? = null,
    val importMessage: String? = null,
)

data class PlaylistSummaryUi(
    val id: Long,
    val displayName: String,
    val sourcePath: String,
    val selectedEntryName: String,
    val lastSeenLabel: String,
)

data class PlaylistDetailUi(
    val id: Long,
    val displayName: String,
    val sourcePath: String,
    val sourcePathLabel: String,
    val entries: List<PlaylistEntryUi>,
)

data class PlaylistEntryUi(
    val index: Int,
    val displayName: String,
    val rawLine: String,
    val resolvedPath: String,
    val selected: Boolean,
)
