package net.aitorciki.dem3ux.ui

import android.graphics.Bitmap

data class Dem3uxUiState(
    val playlists: List<PlaylistSummaryUi> = emptyList(),
    val selectedPlaylist: PlaylistDetailUi? = null,
    val esDeSetup: EsDeSetupUiState = EsDeSetupUiState(),
    val importMessage: String? = null,
)

data class EsDeSetupUiState(
    val customSystemsUri: String? = null,
    val presets: List<EsDeSetupPresetUi> = emptyList(),
) {
    val hasFolderAccess: Boolean = customSystemsUri != null
}

data class EsDeSetupPresetUi(
    val id: String,
    val displayName: String,
    val esDeEmulatorName: String,
    val aliasEntry: String,
    val installed: Boolean,
    val installedTargetIcon: Bitmap?,
    val selected: Boolean,
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
