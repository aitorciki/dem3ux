package net.aitorciki.dem3ux.ui.screens.playlists

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.ui.Dem3uxUiState
import net.aitorciki.dem3ux.ui.PlaylistSummaryUi
import net.aitorciki.dem3ux.ui.preview.PreviewDestinationFrame
import net.aitorciki.dem3ux.ui.preview.previewDetailState

@Composable
internal fun TwoPaneContent(
    uiState: Dem3uxUiState,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistRemoveClick: (PlaylistSummaryUi) -> Unit,
    onEntryClick: (Long, Int) -> Unit,
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        PlaylistList(
            playlists = uiState.playlists,
            playlistsLoaded = uiState.playlistsLoaded,
            selectedPlaylistId = uiState.selectedPlaylist?.id,
            onPlaylistClick = onPlaylistClick,
            onPlaylistRemoveClick = onPlaylistRemoveClick,
            onOpenSetupGuideClick = onOpenSetupGuideClick,
            onOpenSetupClick = onOpenSetupClick,
            modifier = Modifier.weight(0.42f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        Spacer(modifier = Modifier.width(16.dp))
        if (uiState.selectedPlaylist == null) {
            EmptyDetail(modifier = Modifier.weight(0.58f))
        } else {
            PlaylistDetail(
                playlist = uiState.selectedPlaylist,
                onEntryClick = onEntryClick,
                showTitle = true,
                modifier = Modifier.weight(0.58f),
            )
        }
    }
}

@Preview(showBackground = true, name = "Playlists two-pane", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun TwoPaneContentPreview() {
    PreviewDestinationFrame(title = "dem3ux") {
        TwoPaneContent(
            uiState = previewDetailState,
            onPlaylistClick = {},
            onPlaylistRemoveClick = {},
            onEntryClick = { _, _ -> },
            onOpenSetupGuideClick = {},
            onOpenSetupClick = {},
        )
    }
}
