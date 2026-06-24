package net.aitorciki.dem3ux.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.BuildConfig
import net.aitorciki.dem3ux.R
import net.aitorciki.dem3ux.ui.components.AppTopBar
import net.aitorciki.dem3ux.ui.components.PlaylistDetailTopBar
import net.aitorciki.dem3ux.ui.preview.PREVIEW_VERSION_LABEL
import net.aitorciki.dem3ux.ui.preview.previewDetailsById
import net.aitorciki.dem3ux.ui.preview.previewListState
import net.aitorciki.dem3ux.ui.screens.help.HelpContent
import net.aitorciki.dem3ux.ui.screens.playlists.PlaylistDetail
import net.aitorciki.dem3ux.ui.screens.playlists.PlaylistList
import net.aitorciki.dem3ux.ui.screens.playlists.TwoPaneContent
import net.aitorciki.dem3ux.ui.screens.setup.SetupContent
import net.aitorciki.dem3ux.ui.theme.Dem3uxTheme
import org.koin.androidx.compose.koinViewModel

private const val SETUP_GUIDE_URL = "https://github.com/aitorciki/dem3ux#frontend-integration"

private enum class MainDestination {
    Playlists,
    Setup,
    Help,
}

internal enum class SetupStep {
    Frontends,
    EsDe,
}

@Composable
fun Dem3uxApp() {
    val viewModel: Dem3uxViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Dem3uxApp(
        uiState = uiState,
        versionLabel = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        onPlaylistClick = viewModel::selectPlaylist,
        onBackClick = viewModel::clearSelectedPlaylist,
        onPlaylistRemoveClick = viewModel::deletePlaylist,
        onEntryClick = viewModel::selectEntry,
        onOpenPlaylistClick = viewModel::importPlaylist,
        onEsDeFolderSelected = viewModel::selectEsDeCustomSystemsFolder,
        onEsDePresetSelectedChange = viewModel::setEsDePresetSelected,
        onSaveEsDeSetupClick = viewModel::saveEsDeSetup,
        onImportMessageShown = viewModel::clearImportMessage,
    )
}

@Composable
private fun Dem3uxApp(
    uiState: Dem3uxUiState,
    versionLabel: String,
    onPlaylistClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onPlaylistRemoveClick: (Long) -> Unit,
    onEntryClick: (Long, Int) -> Unit,
    onOpenPlaylistClick: (Uri) -> Unit,
    onEsDeFolderSelected: (Uri, Int) -> Unit,
    onEsDePresetSelectedChange: (String, Boolean) -> Unit,
    onSaveEsDeSetupClick: () -> Unit,
    onImportMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var destination by rememberSaveable { mutableStateOf(MainDestination.Playlists) }
    var setupStep by rememberSaveable { mutableStateOf(SetupStep.Frontends) }
    var playlistPendingRemovalId by rememberSaveable { mutableStateOf<Long?>(null) }
    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onOpenPlaylistClick(uri)
            }
        }
    val openEsDeFolderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (uri != null) {
                onEsDeFolderSelected(uri, result.data?.flags ?: 0)
            }
        }

    LaunchedEffect(uiState.importMessage) {
        val message = uiState.importMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onImportMessageShown()
    }

    Dem3uxTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useTwoPane = maxWidth >= 840.dp && maxHeight >= 600.dp
            val selectedPlaylist = uiState.selectedPlaylist
            val playlistPendingRemoval = uiState.playlists.firstOrNull { playlist -> playlist.id == playlistPendingRemovalId }
            val openDrawer: () -> Unit = {
                coroutineScope.launch { drawerState.open() }
            }
            val openSetupGuide: () -> Unit = {
                uriHandler.openUri(SETUP_GUIDE_URL)
            }
            val openSetup: () -> Unit = {
                destination = MainDestination.Setup
                setupStep = SetupStep.Frontends
            }

            BackHandler(enabled = destination == MainDestination.Help || destination == MainDestination.Setup) {
                if (destination == MainDestination.Setup && setupStep != SetupStep.Frontends) {
                    setupStep = SetupStep.Frontends
                } else {
                    destination = MainDestination.Playlists
                }
            }
            BackHandler(enabled = destination == MainDestination.Playlists && !useTwoPane && selectedPlaylist != null) {
                onBackClick()
            }
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            if (playlistPendingRemoval != null) {
                AlertDialog(
                    onDismissRequest = { playlistPendingRemovalId = null },
                    title = { Text("Remove playlist?") },
                    text = {
                        Text(
                            "Remove ${playlistPendingRemoval.displayName} from dem3ux? This does not delete the .m3u or game files.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onPlaylistRemoveClick(playlistPendingRemoval.id)
                                playlistPendingRemovalId = null
                            },
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { playlistPendingRemovalId = null }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Dem3uxDrawer(
                        destination = destination,
                        versionLabel = versionLabel,
                        onDestinationClick = { selectedDestination ->
                            destination = selectedDestination
                            if (selectedDestination == MainDestination.Setup) {
                                setupStep = SetupStep.Frontends
                            }
                            coroutineScope.launch { drawerState.close() }
                        },
                    )
                },
            ) {
                Scaffold(
                    topBar = {
                        when {
                            destination == MainDestination.Help || destination == MainDestination.Setup -> {
                                AppTopBar(
                                    title = if (destination == MainDestination.Help) "Help" else "Setup",
                                    onMenuClick = openDrawer,
                                )
                            }

                            !useTwoPane && selectedPlaylist != null -> {
                                PlaylistDetailTopBar(
                                    title = selectedPlaylist.displayName,
                                    subtitle = selectedPlaylist.sourcePath,
                                    onBackClick = onBackClick,
                                )
                            }

                            else -> {
                                AppTopBar(
                                    title = "dem3ux",
                                    subtitle = if (useTwoPane) null else "Seen playlists",
                                    onMenuClick = openDrawer,
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        if (destination == MainDestination.Playlists && uiState.selectedPlaylist == null) {
                            FloatingActionButton(onClick = { openDocumentLauncher.launch(arrayOf("*/*")) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_open_file),
                                    contentDescription = "Open m3u playlist",
                                )
                            }
                        }
                    },
                ) { contentPadding ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                                    .padding(16.dp),
                        ) {
                            if (destination == MainDestination.Help) {
                                HelpContent(
                                    onOpenSetupGuideClick = openSetupGuide,
                                    onOpenSetupClick = openSetup,
                                    modifier = Modifier.weight(1f),
                                )
                            } else if (destination == MainDestination.Setup) {
                                SetupContent(
                                    setupFrontends = uiState.setupFrontends,
                                    setupStep = setupStep,
                                    setupState = uiState.esDeSetup,
                                    useTwoPane = useTwoPane,
                                    onFrontendClick = { frontendId ->
                                        if (frontendId == SETUP_FRONTEND_ES_DE) {
                                            setupStep = SetupStep.EsDe
                                        }
                                    },
                                    onChooseEsDeFolderClick = { openEsDeFolderLauncher.launch(openDocumentTreeIntent()) },
                                    onPresetSelectedChange = onEsDePresetSelectedChange,
                                    onSaveClick = onSaveEsDeSetupClick,
                                    modifier = Modifier.weight(1f),
                                )
                            } else if (useTwoPane) {
                                TwoPaneContent(
                                    uiState = uiState,
                                    onPlaylistClick = onPlaylistClick,
                                    onPlaylistRemoveClick = { playlist -> playlistPendingRemovalId = playlist.id },
                                    onEntryClick = onEntryClick,
                                    onOpenSetupGuideClick = openSetupGuide,
                                    onOpenSetupClick = openSetup,
                                    modifier = Modifier.weight(1f),
                                )
                            } else if (uiState.selectedPlaylist == null) {
                                PlaylistList(
                                    playlists = uiState.playlists,
                                    playlistsLoaded = uiState.playlistsLoaded,
                                    onPlaylistClick = onPlaylistClick,
                                    onPlaylistRemoveClick = { playlist -> playlistPendingRemovalId = playlist.id },
                                    onOpenSetupGuideClick = openSetupGuide,
                                    onOpenSetupClick = openSetup,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                PlaylistDetail(
                                    playlist = uiState.selectedPlaylist,
                                    onEntryClick = onEntryClick,
                                    showTitle = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dem3uxDrawer(
    destination: MainDestination,
    versionLabel: String,
    onDestinationClick: (MainDestination) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight()) {
            Text(
                text = "dem3ux",
                modifier = Modifier.padding(28.dp, 24.dp, 28.dp, 12.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            NavigationDrawerItem(
                label = { Text("Playlists") },
                selected = destination == MainDestination.Playlists,
                onClick = { onDestinationClick(MainDestination.Playlists) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Setup") },
                selected = destination == MainDestination.Setup,
                onClick = { onDestinationClick(MainDestination.Setup) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Help") },
                selected = destination == MainDestination.Help,
                onClick = { onDestinationClick(MainDestination.Help) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = versionLabel,
                modifier = Modifier.padding(28.dp, 12.dp, 28.dp, 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openDocumentTreeIntent(): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
        )
    }

@Preview(showBackground = true, name = "Interactive light")
@Preview(showBackground = true, name = "Interactive dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, name = "Interactive phone landscape", device = "spec:width=891dp,height=411dp,dpi=420")
@Preview(showBackground = true, name = "Interactive tablet landscape", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun Dem3uxAppInteractivePreview() {
    var uiState by remember { mutableStateOf(previewListState) }

    Dem3uxApp(
        uiState = uiState,
        versionLabel = PREVIEW_VERSION_LABEL,
        onPlaylistClick = { playlistId ->
            uiState = uiState.copy(selectedPlaylist = previewDetailsById[playlistId])
        },
        onBackClick = {
            uiState = uiState.copy(selectedPlaylist = null)
        },
        onPlaylistRemoveClick = { playlistId ->
            uiState =
                uiState.copy(
                    playlists = uiState.playlists.filterNot { playlist -> playlist.id == playlistId },
                    selectedPlaylist = uiState.selectedPlaylist?.takeUnless { playlist -> playlist.id == playlistId },
                )
        },
        onEntryClick = onEntryClick@{ playlistId, entryIndex ->
            val selectedPlaylist = uiState.selectedPlaylist ?: return@onEntryClick
            if (selectedPlaylist.id != playlistId) {
                return@onEntryClick
            }

            val updatedEntries =
                selectedPlaylist.entries.map { entry ->
                    entry.copy(selected = entry.index == entryIndex)
                }
            val selectedEntryName = updatedEntries.firstOrNull { entry -> entry.selected }?.displayName

            uiState =
                uiState.copy(
                    playlists =
                        uiState.playlists.map { playlist ->
                            if (playlist.id == playlistId && selectedEntryName != null) {
                                playlist.copy(selectedEntryName = selectedEntryName)
                            } else {
                                playlist
                            }
                        },
                    selectedPlaylist = selectedPlaylist.copy(entries = updatedEntries),
                )
        },
        onOpenPlaylistClick = {},
        onEsDeFolderSelected = { _, _ -> },
        onEsDePresetSelectedChange = { _, _ -> },
        onSaveEsDeSetupClick = {},
        onImportMessageShown = {},
    )
}
