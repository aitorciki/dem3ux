package net.aitorciki.dem3ux.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.aitorciki.dem3ux.ui.Dem3uxUiState
import net.aitorciki.dem3ux.ui.EsDeSetupPresetUi
import net.aitorciki.dem3ux.ui.EsDeSetupUiState
import net.aitorciki.dem3ux.ui.PlaylistDetailUi
import net.aitorciki.dem3ux.ui.PlaylistEntryUi
import net.aitorciki.dem3ux.ui.PlaylistSummaryUi
import net.aitorciki.dem3ux.ui.SETUP_FRONTEND_ES_DE
import net.aitorciki.dem3ux.ui.SetupFrontendUi
import net.aitorciki.dem3ux.ui.components.AppTopBar

@Composable
internal fun PreviewDestinationFrame(
    title: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                onMenuClick = {},
            )
        },
    ) { contentPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
internal fun PreviewScreenFrame(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            content()
        }
    }
}

internal val previewPlaylists =
    listOf(
        PlaylistSummaryUi(
            id = 1,
            displayName = "Nebula Drift (Demo)",
            sourcePath = "/storage/emulated/0/roms/psx/Nebula Drift (Demo).m3u",
            selectedEntryName = "Nebula Drift (Demo) (Disc 2)",
            lastSeenLabel = "Last accessed today",
        ),
        PlaylistSummaryUi(
            id = 2,
            displayName = "Arcade Sampler",
            sourcePath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FArcade%20Sampler.m3u",
            selectedEntryName = "Arcade Sampler (Disc 1)",
            lastSeenLabel = "Last accessed yesterday",
        ),
    )

internal val previewNebulaDriftDetail =
    PlaylistDetailUi(
        id = 1,
        displayName = "Nebula Drift (Demo)",
        sourcePath = "/storage/emulated/0/roms/psx/Nebula Drift (Demo).m3u",
        sourcePathLabel = ".../roms/psx/Nebula Drift (Demo).m3u",
        entries =
            listOf(
                PlaylistEntryUi(
                    index = 0,
                    displayName = "Nebula Drift (Demo) (Disc 1)",
                    rawLine = ".Nebula Drift (Demo)/Nebula Drift (Demo) (Disc 1).chd",
                    resolvedPath = "/storage/emulated/0/roms/psx/.Nebula Drift (Demo)/Nebula Drift (Demo) (Disc 1).chd",
                    selected = false,
                ),
                PlaylistEntryUi(
                    index = 1,
                    displayName = "Nebula Drift (Demo) (Disc 2)",
                    rawLine = ".Nebula Drift (Demo)/Nebula Drift (Demo) (Disc 2).chd",
                    resolvedPath = "/storage/emulated/0/roms/psx/.Nebula Drift (Demo)/Nebula Drift (Demo) (Disc 2).chd",
                    selected = true,
                ),
            ),
    )

internal val previewArcadeSamplerDetail =
    PlaylistDetailUi(
        id = 2,
        displayName = "Arcade Sampler",
        sourcePath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FArcade%20Sampler.m3u",
        sourcePathLabel = ".../roms/psx/Arcade Sampler.m3u",
        entries =
            listOf(
                PlaylistEntryUi(
                    index = 0,
                    displayName = "Arcade Sampler (Disc 1)",
                    rawLine = ".Arcade Sampler/Arcade Sampler (Disc 1).chd",
                    resolvedPath =
                        "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2F.Arcade%20Sampler%2FArcade%20Sampler%20(Disc%201).chd",
                    selected = true,
                ),
                PlaylistEntryUi(
                    index = 1,
                    displayName = "Arcade Sampler (Disc 2)",
                    rawLine = ".Arcade Sampler/Arcade Sampler (Disc 2).chd",
                    resolvedPath =
                        "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2F.Arcade%20Sampler%2FArcade%20Sampler%20(Disc%202).chd",
                    selected = false,
                ),
            ),
    )

internal val previewDetailsById =
    mapOf(
        previewNebulaDriftDetail.id to previewNebulaDriftDetail,
        previewArcadeSamplerDetail.id to previewArcadeSamplerDetail,
    )

internal val previewSetupFrontends =
    listOf(
        SetupFrontendUi(
            id = SETUP_FRONTEND_ES_DE,
            displayName = "ES-DE",
            description = "Configure supported emulator presets",
            installed = true,
            installedIcon = null,
        ),
        SetupFrontendUi(
            id = "daijishou",
            displayName = "Daijishou",
            description = "Configure supported player definitions",
            installed = true,
            installedIcon = null,
        ),
        SetupFrontendUi(
            id = "pegasus",
            displayName = "Pegasus",
            description = "Configure supported launcher entries",
            installed = false,
            installedIcon = null,
        ),
    )

internal val previewListState =
    Dem3uxUiState(
        playlists = previewPlaylists,
        playlistsLoaded = true,
        setupFrontends = previewSetupFrontends,
    )

internal val previewDetailState =
    Dem3uxUiState(
        playlists = previewPlaylists,
        playlistsLoaded = true,
        selectedPlaylist = previewNebulaDriftDetail,
        setupFrontends = previewSetupFrontends,
    )

internal val previewSetupState =
    EsDeSetupUiState(
        customSystemsUri =
            "content://com.android.externalstorage.documents/tree/primary%3AES-DE%2Fcustom_systems",
        presets =
            listOf(
                EsDeSetupPresetUi(
                    id = "duckstation",
                    displayName = "DuckStation",
                    esDeEmulatorName = "DUCKSTATION",
                    aliasEntry = "net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity",
                    installed = true,
                    installedTargetIcon = null,
                    selected = true,
                ),
                EsDeSetupPresetUi(
                    id = "flycast",
                    displayName = "Flycast",
                    esDeEmulatorName = "FLYCAST",
                    aliasEntry = "net.aitorciki.dem3ux/.presets.FlycastBridgeActivity",
                    installed = true,
                    installedTargetIcon = null,
                    selected = false,
                ),
                EsDeSetupPresetUi(
                    id = "redream",
                    displayName = "Redream",
                    esDeEmulatorName = "REDREAM",
                    aliasEntry = "net.aitorciki.dem3ux/.presets.RedreamBridgeActivity",
                    installed = false,
                    installedTargetIcon = null,
                    selected = false,
                ),
            ),
    )

internal const val PREVIEW_VERSION_LABEL = "Version 1.0.0 (1)"
