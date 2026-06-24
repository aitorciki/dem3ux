package net.aitorciki.dem3ux.ui.screens.playlists

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.ui.PlaylistDetailUi
import net.aitorciki.dem3ux.ui.PlaylistEntryUi
import net.aitorciki.dem3ux.ui.components.TrailingSelectionControl
import net.aitorciki.dem3ux.ui.preview.PreviewScreenFrame
import net.aitorciki.dem3ux.ui.preview.previewNebulaDriftDetail
import net.aitorciki.dem3ux.ui.theme.Dem3uxTheme
import net.aitorciki.dem3ux.ui.theme.LIST_ITEM_COLOR_ANIMATION_MILLIS
import net.aitorciki.dem3ux.ui.theme.ListItemGap
import net.aitorciki.dem3ux.ui.theme.animatedListCardShape

@Composable
internal fun PlaylistDetail(
    playlist: PlaylistDetailUi,
    onEntryClick: (Long, Int) -> Unit,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (showTitle) {
            Text(
                text = playlist.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = playlist.sourcePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = "Tap an entry to make it the default for the next bridge launch.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(ListItemGap)) {
            itemsIndexed(playlist.entries, key = { _, entry -> entry.index }) { index, entry ->
                PlaylistEntryRow(
                    entry = entry,
                    shape =
                        animatedListCardShape(
                            index = index,
                            count = playlist.entries.size,
                            selected = entry.selected,
                        ),
                    onClick = { onEntryClick(playlist.id, entry.index) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistEntryRow(
    entry: PlaylistEntryUi,
    shape: Shape,
    onClick: () -> Unit,
) {
    val containerColor by
        animateColorAsState(
            targetValue =
                if (entry.selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            animationSpec = tween(durationMillis = LIST_ITEM_COLOR_ANIMATION_MILLIS),
            label = "playlistEntryCardContainerColor",
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Text(
                    text = entry.rawLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TrailingSelectionControl {
                RadioButton(
                    selected = entry.selected,
                    onClick = onClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Playlist detail")
@Composable
private fun PlaylistDetailPreview() {
    Dem3uxTheme {
        PreviewScreenFrame {
            PlaylistDetail(
                playlist = previewNebulaDriftDetail,
                onEntryClick = { _, _ -> },
                showTitle = true,
            )
        }
    }
}
