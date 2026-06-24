package net.aitorciki.dem3ux.ui.screens.playlists

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.R
import net.aitorciki.dem3ux.ui.PlaylistSummaryUi
import net.aitorciki.dem3ux.ui.components.TrailingSelectionControl
import net.aitorciki.dem3ux.ui.preview.PreviewDestinationFrame
import net.aitorciki.dem3ux.ui.preview.previewPlaylists
import net.aitorciki.dem3ux.ui.theme.DropdownMenuCorner
import net.aitorciki.dem3ux.ui.theme.LIST_ITEM_COLOR_ANIMATION_MILLIS
import net.aitorciki.dem3ux.ui.theme.ListItemGap
import net.aitorciki.dem3ux.ui.theme.animatedListCardShape

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PlaylistList(
    playlists: List<PlaylistSummaryUi>,
    playlistsLoaded: Boolean,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistRemoveClick: (PlaylistSummaryUi) -> Unit,
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedPlaylistId: Long? = null,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val searchScrollsWithList = maxHeight < 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            if (!playlistsLoaded) {
                // Avoid flashing the true empty state before Room emits the first playlist list.
            } else if (playlists.isEmpty()) {
                EmptyPlaylistList(
                    onOpenSetupGuideClick = onOpenSetupGuideClick,
                    onOpenSetupClick = onOpenSetupClick,
                )
            } else {
                val trimmedSearchQuery = searchQuery.trim()
                val filteredPlaylists =
                    if (trimmedSearchQuery.isEmpty()) {
                        playlists
                    } else {
                        playlists.filter { playlist ->
                            playlist.displayName.contains(trimmedSearchQuery, ignoreCase = true)
                        }
                    }

                if (!searchScrollsWithList) {
                    PlaylistSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearch = {
                            searchQuery = it
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        onClearSearchClick = { searchQuery = "" },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(ListItemGap)) {
                    if (searchScrollsWithList) {
                        item(key = "playlist-search") {
                            PlaylistSearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onSearch = {
                                    searchQuery = it
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                onClearSearchClick = { searchQuery = "" },
                            )
                        }
                        item(key = "playlist-search-gap") {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (filteredPlaylists.isEmpty()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.no_playlists_match_search),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        itemsIndexed(filteredPlaylists, key = { _, playlist -> playlist.id }) { index, playlist ->
                            val selected = playlist.id == selectedPlaylistId
                            PlaylistCard(
                                playlist = playlist,
                                selected = selected,
                                shape =
                                    animatedListCardShape(
                                        index = index,
                                        count = filteredPlaylists.size,
                                        selected = selected,
                                    ),
                                onClick = { onPlaylistClick(playlist.id) },
                                onRemoveClick = { onPlaylistRemoveClick(playlist) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaylistSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearchClick: () -> Unit,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.search_playlists)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearchClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_search_cd),
                            )
                        }
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier.fillMaxWidth(),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {}
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistSummaryUi,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val containerColor by
        animateColorAsState(
            targetValue =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            animationSpec = tween(durationMillis = LIST_ITEM_COLOR_ANIMATION_MILLIS),
            label = "playlistCardContainerColor",
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playlist.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier.width(20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TrailingSelectionControl {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.playlist_options_cd),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(DropdownMenuCorner),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                menuExpanded = false
                                onRemoveClick()
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }
                }
            }
            Text(
                text = playlist.sourcePath,
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.selected_entry_label, playlist.selectedEntryName),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlist.lastSeenLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Playlist list")
@Composable
private fun PlaylistListPreview() {
    PreviewDestinationFrame(title = "dem3ux") {
        PlaylistList(
            playlists = previewPlaylists,
            playlistsLoaded = true,
            onPlaylistClick = {},
            onPlaylistRemoveClick = {},
            onOpenSetupGuideClick = {},
            onOpenSetupClick = {},
        )
    }
}
