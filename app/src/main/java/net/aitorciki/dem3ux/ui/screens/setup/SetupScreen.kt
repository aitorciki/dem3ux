package net.aitorciki.dem3ux.ui.screens.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.ui.EsDeSetupUiState
import net.aitorciki.dem3ux.ui.SETUP_FRONTEND_ES_DE
import net.aitorciki.dem3ux.ui.SetupFrontendUi
import net.aitorciki.dem3ux.ui.SetupStep
import net.aitorciki.dem3ux.ui.preview.PreviewDestinationFrame
import net.aitorciki.dem3ux.ui.preview.previewSetupFrontends
import net.aitorciki.dem3ux.ui.preview.previewSetupState
import net.aitorciki.dem3ux.ui.theme.LIST_ITEM_COLOR_ANIMATION_MILLIS
import net.aitorciki.dem3ux.ui.theme.ListItemGap
import net.aitorciki.dem3ux.ui.theme.animatedListCardShape

private val SetupStep.selectedFrontendId: String?
    get() =
        when (this) {
            SetupStep.Frontends -> null
            SetupStep.EsDe -> SETUP_FRONTEND_ES_DE
        }

@Composable
internal fun SetupContent(
    setupFrontends: List<SetupFrontendUi>,
    setupStep: SetupStep,
    setupState: EsDeSetupUiState,
    useTwoPane: Boolean,
    onFrontendClick: (String) -> Unit,
    onChooseEsDeFolderClick: () -> Unit,
    onPresetSelectedChange: (String, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (useTwoPane) {
        SetupTwoPaneContent(
            setupFrontends = setupFrontends,
            selectedFrontendId = setupStep.selectedFrontendId,
            setupState = setupState,
            onFrontendClick = onFrontendClick,
            onChooseEsDeFolderClick = onChooseEsDeFolderClick,
            onPresetSelectedChange = onPresetSelectedChange,
            onSaveClick = onSaveClick,
            modifier = modifier,
        )
        return
    }

    when (setupStep) {
        SetupStep.Frontends -> {
            SetupFrontendListContent(
                setupFrontends = setupFrontends,
                selectedFrontendId = null,
                onFrontendClick = onFrontendClick,
                modifier = modifier,
            )
        }

        SetupStep.EsDe -> {
            EsDeSetupContent(
                setupState = setupState,
                onChooseEsDeFolderClick = onChooseEsDeFolderClick,
                onPresetSelectedChange = onPresetSelectedChange,
                onSaveClick = onSaveClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun SetupTwoPaneContent(
    setupFrontends: List<SetupFrontendUi>,
    selectedFrontendId: String?,
    setupState: EsDeSetupUiState,
    onFrontendClick: (String) -> Unit,
    onChooseEsDeFolderClick: () -> Unit,
    onPresetSelectedChange: (String, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        SetupFrontendListContent(
            setupFrontends = setupFrontends,
            selectedFrontendId = selectedFrontendId,
            onFrontendClick = onFrontendClick,
            modifier = Modifier.weight(0.42f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        Spacer(modifier = Modifier.width(16.dp))
        if (selectedFrontendId == SETUP_FRONTEND_ES_DE) {
            EsDeSetupContent(
                setupState = setupState,
                onChooseEsDeFolderClick = onChooseEsDeFolderClick,
                onPresetSelectedChange = onPresetSelectedChange,
                onSaveClick = onSaveClick,
                modifier = Modifier.weight(0.58f),
            )
        } else {
            EmptySetupDetail(modifier = Modifier.weight(0.58f))
        }
    }
}

@Composable
internal fun SetupFrontendListContent(
    setupFrontends: List<SetupFrontendUi>,
    selectedFrontendId: String?,
    onFrontendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ListItemGap),
    ) {
        item {
            Text(
                text = "Choose a supported frontend to configure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemGap)) {
                setupFrontends.forEachIndexed { index, frontend ->
                    SetupFrontendRow(
                        frontend = frontend,
                        selected = frontend.id == selectedFrontendId,
                        shape =
                            animatedListCardShape(
                                index = index,
                                count = setupFrontends.size,
                                selected = frontend.id == selectedFrontendId,
                            ),
                        onClick = { onFrontendClick(frontend.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupFrontendRow(
    frontend: SetupFrontendUi,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    val enabled = frontend.installed
    val containerColor by
        animateColorAsState(
            targetValue =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            animationSpec = tween(durationMillis = LIST_ITEM_COLOR_ANIMATION_MILLIS),
            label = "setupFrontendCardContainerColor",
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
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (frontend.installedIcon != null) {
                Image(
                    bitmap = frontend.installedIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = frontend.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun EmptySetupDetail(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Select a frontend",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Frontend-specific emulator configuration will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, name = "Setup")
@Composable
private fun SetupContentPreview() {
    PreviewDestinationFrame(title = "Setup") {
        SetupContent(
            setupFrontends = previewSetupFrontends,
            setupStep = SetupStep.Frontends,
            setupState = previewSetupState,
            useTwoPane = false,
            onFrontendClick = {},
            onChooseEsDeFolderClick = {},
            onPresetSelectedChange = { _, _ -> },
            onSaveClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Setup tablet landscape", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun SetupContentTabletLandscapePreview() {
    PreviewDestinationFrame(title = "Setup") {
        SetupContent(
            setupFrontends = previewSetupFrontends,
            setupStep = SetupStep.EsDe,
            setupState = previewSetupState,
            useTwoPane = true,
            onFrontendClick = {},
            onChooseEsDeFolderClick = {},
            onPresetSelectedChange = { _, _ -> },
            onSaveClick = {},
        )
    }
}
