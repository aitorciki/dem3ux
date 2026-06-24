package net.aitorciki.dem3ux.ui.screens.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.ui.EsDeSetupPresetUi
import net.aitorciki.dem3ux.ui.EsDeSetupUiState
import net.aitorciki.dem3ux.ui.components.TrailingSelectionControl
import net.aitorciki.dem3ux.ui.preview.PreviewDestinationFrame
import net.aitorciki.dem3ux.ui.preview.previewSetupState
import net.aitorciki.dem3ux.ui.theme.LIST_ITEM_COLOR_ANIMATION_MILLIS
import net.aitorciki.dem3ux.ui.theme.ListItemGap
import net.aitorciki.dem3ux.ui.theme.animatedListCardShape

@Composable
internal fun EsDeSetupContent(
    setupState: EsDeSetupUiState,
    onChooseEsDeFolderClick: () -> Unit,
    onPresetSelectedChange: (String, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var setupCardExpanded by remember { mutableStateOf(!setupState.hasFolderAccess) }

    LaunchedEffect(setupState.hasFolderAccess) {
        setupCardExpanded = !setupState.hasFolderAccess
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ES-DE preset setup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (!setupCardExpanded && setupState.customSystemsUri != null) {
                                    Text(
                                        text = setupState.customSystemsUri,
                                        modifier = Modifier.basicMarquee(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (setupState.hasFolderAccess) {
                                TextButton(onClick = { setupCardExpanded = !setupCardExpanded }) {
                                    Text(if (setupCardExpanded) "Hide" else "Show")
                                }
                            }
                        }
                        if (setupCardExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select ES-DE's custom_systems folder, then choose which installed emulators dem3ux should wrap.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onChooseEsDeFolderClick) {
                                Text("Select custom_systems folder")
                            }
                        }
                        if (setupCardExpanded && setupState.customSystemsUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = setupState.customSystemsUri,
                                modifier = Modifier.basicMarquee(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            val installedPresets = setupState.presets.filter { preset -> preset.installed }
            if (installedPresets.isEmpty()) {
                item {
                    Text(
                        text = "No supported emulator targets were detected. Install a supported emulator, then reopen this screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemGap)) {
                        installedPresets.forEachIndexed { index, preset ->
                            EsDePresetRow(
                                preset = preset,
                                enabled = setupState.hasFolderAccess,
                                shape =
                                    animatedListCardShape(
                                        index = index,
                                        count = installedPresets.size,
                                        selected = preset.selected,
                                    ),
                                onSelectedChange = { selected -> onPresetSelectedChange(preset.id, selected) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSaveClick,
            enabled = setupState.hasFolderAccess,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save ES-DE setup")
        }
    }
}

@Composable
private fun EsDePresetRow(
    preset: EsDeSetupPresetUi,
    enabled: Boolean,
    shape: Shape,
    onSelectedChange: (Boolean) -> Unit,
) {
    val containerColor by
        animateColorAsState(
            targetValue =
                if (preset.selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            animationSpec = tween(durationMillis = LIST_ITEM_COLOR_ANIMATION_MILLIS),
            label = "presetCardContainerColor",
        )

    Card(
        modifier = Modifier.alpha(if (enabled) 1f else 0.56f),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = shape,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onSelectedChange(!preset.selected) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (preset.installedTargetIcon != null) {
                Image(
                    bitmap = preset.installedTargetIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            TrailingSelectionControl {
                Checkbox(
                    checked = preset.selected,
                    onCheckedChange = onSelectedChange,
                    enabled = enabled,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "ES-DE setup")
@Composable
private fun EsDeSetupContentPreview() {
    PreviewDestinationFrame(title = "Setup") {
        EsDeSetupContent(
            setupState = previewSetupState,
            onChooseEsDeFolderClick = {},
            onPresetSelectedChange = { _, _ -> },
            onSaveClick = {},
        )
    }
}
