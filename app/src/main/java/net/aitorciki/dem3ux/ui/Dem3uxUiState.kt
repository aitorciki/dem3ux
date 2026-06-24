package net.aitorciki.dem3ux.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class Dem3uxUiState(
    val playlists: List<PlaylistSummaryUi> = emptyList(),
    val playlistsLoaded: Boolean = false,
    val selectedPlaylist: PlaylistDetailUi? = null,
    val setupFrontends: List<SetupFrontendUi> = emptyList(),
    val esDeSetup: EsDeSetupUiState = EsDeSetupUiState(),
    val importMessage: String? = null,
)

@Stable
data class SetupFrontendUi(
    val id: String,
    val displayName: String,
    val description: String,
    val installed: Boolean,
    val installedIcon: Bitmap?,
)

@Immutable
data class EsDeSetupUiState(
    val customSystemsUri: String? = null,
    val presets: List<EsDeSetupPresetUi> = emptyList(),
) {
    val hasFolderAccess: Boolean = customSystemsUri != null
}

@Stable
data class EsDeSetupPresetUi(
    val id: String,
    val displayName: String,
    val esDeEmulatorName: String,
    val aliasEntry: String,
    val installed: Boolean,
    val installedTargetIcon: Bitmap?,
    val selected: Boolean,
)

@Immutable
data class PlaylistSummaryUi(
    val id: Long,
    val displayName: String,
    val sourcePath: String,
    val selectedEntryName: String,
    val lastSeenLabel: String,
)

@Immutable
data class PlaylistDetailUi(
    val id: Long,
    val displayName: String,
    val sourcePath: String,
    val sourcePathLabel: String,
    val entries: List<PlaylistEntryUi>,
)

@Immutable
data class PlaylistEntryUi(
    val index: Int,
    val displayName: String,
    val rawLine: String,
    val resolvedPath: String,
    val selected: Boolean,
)
