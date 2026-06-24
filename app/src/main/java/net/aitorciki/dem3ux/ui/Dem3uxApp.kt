package net.aitorciki.dem3ux.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.BuildConfig
import net.aitorciki.dem3ux.R
import net.aitorciki.dem3ux.ui.components.AppTopBar
import net.aitorciki.dem3ux.ui.components.PlaylistDetailTopBar
import net.aitorciki.dem3ux.ui.nav.HelpRoute
import net.aitorciki.dem3ux.ui.nav.MainRoute
import net.aitorciki.dem3ux.ui.nav.PlaylistsRoute
import net.aitorciki.dem3ux.ui.nav.SetupRoute
import net.aitorciki.dem3ux.ui.preview.PREVIEW_VERSION_LABEL
import net.aitorciki.dem3ux.ui.preview.previewDetailsById
import net.aitorciki.dem3ux.ui.preview.previewListState
import net.aitorciki.dem3ux.ui.preview.previewSetupFrontends
import net.aitorciki.dem3ux.ui.preview.previewSetupState
import net.aitorciki.dem3ux.ui.screens.help.HelpContent
import net.aitorciki.dem3ux.ui.screens.playlists.PlaylistDetail
import net.aitorciki.dem3ux.ui.screens.playlists.PlaylistList
import net.aitorciki.dem3ux.ui.screens.playlists.TwoPaneContent
import net.aitorciki.dem3ux.ui.screens.setup.SetupContent
import net.aitorciki.dem3ux.ui.theme.Dem3uxTheme
import org.koin.androidx.compose.koinViewModel

private const val SETUP_GUIDE_URL = "https://github.com/aitorciki/dem3ux#frontend-integration"

private const val FADE_MILLIS = 150

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
    val navController = rememberNavController()
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
            val navigateToRoute: (MainRoute) -> Unit = { route ->
                if (route == SetupRoute) {
                    setupStep = SetupStep.Frontends
                }
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                coroutineScope.launch { drawerState.close() }
            }
            val openSetup: () -> Unit = { navigateToRoute(SetupRoute) }

            val currentRoute = navController.currentRoute()

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
                        currentRoute = currentRoute,
                        versionLabel = versionLabel,
                        onRouteClick = navigateToRoute,
                    )
                },
            ) {
                Scaffold(
                    topBar = {
                        when (currentRoute) {
                            HelpRoute, SetupRoute -> {
                                AppTopBar(
                                    title = if (currentRoute == HelpRoute) "Help" else "Setup",
                                    onMenuClick = openDrawer,
                                )
                            }

                            PlaylistsRoute -> {
                                if (!useTwoPane && selectedPlaylist != null) {
                                    PlaylistDetailTopBar(
                                        title = selectedPlaylist.displayName,
                                        subtitle = selectedPlaylist.sourcePath,
                                        onBackClick = onBackClick,
                                    )
                                } else {
                                    AppTopBar(
                                        title = "dem3ux",
                                        subtitle = if (useTwoPane) null else "Seen playlists",
                                        onMenuClick = openDrawer,
                                    )
                                }
                            }

                            null -> {
                                AppTopBar(
                                    title = "dem3ux",
                                    onMenuClick = openDrawer,
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        if (currentRoute == PlaylistsRoute && uiState.selectedPlaylist == null) {
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
                            NavHost(
                                navController = navController,
                                startDestination = PlaylistsRoute,
                                enterTransition = { fadeIn(tween(FADE_MILLIS)) },
                                exitTransition = { fadeOut(tween(FADE_MILLIS)) },
                                popEnterTransition = { fadeIn(tween(FADE_MILLIS)) },
                                popExitTransition = { fadeOut(tween(FADE_MILLIS)) },
                            ) {
                                composable<PlaylistsRoute> {
                                    BackHandler(enabled = !useTwoPane && selectedPlaylist != null) {
                                        onBackClick()
                                    }

                                    if (useTwoPane) {
                                        TwoPaneContent(
                                            uiState = uiState,
                                            onPlaylistClick = onPlaylistClick,
                                            onPlaylistRemoveClick = { playlist -> playlistPendingRemovalId = playlist.id },
                                            onEntryClick = onEntryClick,
                                            onOpenSetupGuideClick = openSetupGuide,
                                            onOpenSetupClick = openSetup,
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        AnimatedContent(
                                            targetState = uiState.selectedPlaylist,
                                            modifier = Modifier.weight(1f),
                                            transitionSpec = { fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS)) },
                                            contentKey = { playlist -> playlist?.id ?: -1L },
                                            label = "playlistSinglePaneContent",
                                        ) { playlist ->
                                            if (playlist == null) {
                                                PlaylistList(
                                                    playlists = uiState.playlists,
                                                    playlistsLoaded = uiState.playlistsLoaded,
                                                    onPlaylistClick = onPlaylistClick,
                                                    onPlaylistRemoveClick = { item -> playlistPendingRemovalId = item.id },
                                                    onOpenSetupGuideClick = openSetupGuide,
                                                    onOpenSetupClick = openSetup,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            } else {
                                                PlaylistDetail(
                                                    playlist = playlist,
                                                    onEntryClick = onEntryClick,
                                                    showTitle = false,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                        }
                                    }
                                }
                                composable<SetupRoute> {
                                    BackHandler(enabled = setupStep != SetupStep.Frontends) {
                                        setupStep = SetupStep.Frontends
                                    }

                                    if (useTwoPane) {
                                        SetupContent(
                                            setupFrontends = uiState.setupFrontends,
                                            setupStep = setupStep,
                                            setupState = uiState.esDeSetup,
                                            useTwoPane = true,
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
                                    } else {
                                        AnimatedContent(
                                            targetState = setupStep,
                                            modifier = Modifier.weight(1f),
                                            transitionSpec = { fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS)) },
                                            label = "setupSinglePaneContent",
                                        ) { targetSetupStep ->
                                            SetupContent(
                                                setupFrontends = uiState.setupFrontends,
                                                setupStep = targetSetupStep,
                                                setupState = uiState.esDeSetup,
                                                useTwoPane = false,
                                                onFrontendClick = { frontendId ->
                                                    if (frontendId == SETUP_FRONTEND_ES_DE) {
                                                        setupStep = SetupStep.EsDe
                                                    }
                                                },
                                                onChooseEsDeFolderClick = { openEsDeFolderLauncher.launch(openDocumentTreeIntent()) },
                                                onPresetSelectedChange = onEsDePresetSelectedChange,
                                                onSaveClick = onSaveEsDeSetupClick,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                                composable<HelpRoute> {
                                    HelpContent(
                                        onOpenSetupGuideClick = openSetupGuide,
                                        onOpenSetupClick = openSetup,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.navigation.NavController.currentRoute(): MainRoute? = currentBackStackEntryAsState().value?.destination?.toMainRoute()

private fun NavDestination.toMainRoute(): MainRoute? =
    when (route) {
        PlaylistsRoute::class.qualifiedName -> PlaylistsRoute
        SetupRoute::class.qualifiedName -> SetupRoute
        HelpRoute::class.qualifiedName -> HelpRoute
        else -> null
    }

@Composable
private fun Dem3uxDrawer(
    currentRoute: MainRoute?,
    versionLabel: String,
    onRouteClick: (MainRoute) -> Unit,
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
                selected = currentRoute == PlaylistsRoute,
                onClick = { onRouteClick(PlaylistsRoute) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Setup") },
                selected = currentRoute == SetupRoute,
                onClick = { onRouteClick(SetupRoute) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Help") },
                selected = currentRoute == HelpRoute,
                onClick = { onRouteClick(HelpRoute) },
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
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    Dem3uxTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useTwoPane = maxWidth >= 840.dp && maxHeight >= 600.dp
            val currentRoute = navController.currentRoute()
            val openDrawer: () -> Unit = { coroutineScope.launch { drawerState.open() } }

            val navigateToRoute: (MainRoute) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                coroutineScope.launch { drawerState.close() }
            }

            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Dem3uxDrawer(
                        currentRoute = currentRoute,
                        versionLabel = PREVIEW_VERSION_LABEL,
                        onRouteClick = navigateToRoute,
                    )
                },
            ) {
                Scaffold(
                    topBar = {
                        val selectedPlaylist = uiState.selectedPlaylist
                        when (currentRoute) {
                            HelpRoute, SetupRoute -> {
                                AppTopBar(
                                    title = if (currentRoute == HelpRoute) "Help" else "Setup",
                                    onMenuClick = openDrawer,
                                )
                            }

                            PlaylistsRoute -> {
                                if (!useTwoPane && selectedPlaylist != null) {
                                    PlaylistDetailTopBar(
                                        title = selectedPlaylist.displayName,
                                        subtitle = selectedPlaylist.sourcePath,
                                        onBackClick = {
                                            uiState = uiState.copy(selectedPlaylist = null)
                                        },
                                    )
                                } else {
                                    AppTopBar(
                                        title = "dem3ux",
                                        subtitle = if (useTwoPane) null else "Seen playlists",
                                        onMenuClick = openDrawer,
                                    )
                                }
                            }

                            null -> {
                                AppTopBar(title = "dem3ux", onMenuClick = openDrawer)
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentRoute == PlaylistsRoute && uiState.selectedPlaylist == null) {
                            FloatingActionButton(onClick = {}) {
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
                            NavHost(
                                navController = navController,
                                startDestination = PlaylistsRoute,
                                enterTransition = { fadeIn(tween(FADE_MILLIS)) },
                                exitTransition = { fadeOut(tween(FADE_MILLIS)) },
                                popEnterTransition = { fadeIn(tween(FADE_MILLIS)) },
                                popExitTransition = { fadeOut(tween(FADE_MILLIS)) },
                            ) {
                                composable<PlaylistsRoute> {
                                    val selectedPlaylist = uiState.selectedPlaylist
                                    BackHandler(enabled = !useTwoPane && selectedPlaylist != null) {
                                        uiState = uiState.copy(selectedPlaylist = null)
                                    }

                                    val onEntryClick: (Long, Int) -> Unit = { playlistId, entryIndex ->
                                        val sp = selectedPlaylist
                                        if (sp != null && sp.id == playlistId) {
                                            val updatedEntries =
                                                sp.entries.map { entry -> entry.copy(selected = entry.index == entryIndex) }
                                            val selectedEntryName =
                                                updatedEntries.firstOrNull { entry -> entry.selected }?.displayName
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
                                                    selectedPlaylist = sp.copy(entries = updatedEntries),
                                                )
                                        }
                                    }

                                    if (useTwoPane) {
                                        TwoPaneContent(
                                            uiState = uiState,
                                            onPlaylistClick = { playlistId ->
                                                uiState = uiState.copy(selectedPlaylist = previewDetailsById[playlistId])
                                            },
                                            onPlaylistRemoveClick = { playlist ->
                                                uiState =
                                                    uiState.copy(
                                                        playlists = uiState.playlists.filterNot { p -> p.id == playlist.id },
                                                        selectedPlaylist =
                                                            uiState.selectedPlaylist?.takeUnless { p ->
                                                                p.id == playlist.id
                                                            },
                                                    )
                                            },
                                            onEntryClick = onEntryClick,
                                            onOpenSetupGuideClick = {},
                                            onOpenSetupClick = { navigateToRoute(SetupRoute) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        AnimatedContent(
                                            targetState = selectedPlaylist,
                                            modifier = Modifier.weight(1f),
                                            transitionSpec = { fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS)) },
                                            contentKey = { playlist -> playlist?.id ?: -1L },
                                            label = "previewPlaylistSinglePaneContent",
                                        ) { playlist ->
                                            if (playlist == null) {
                                                PlaylistList(
                                                    playlists = uiState.playlists,
                                                    playlistsLoaded = true,
                                                    onPlaylistClick = { playlistId ->
                                                        uiState = uiState.copy(selectedPlaylist = previewDetailsById[playlistId])
                                                    },
                                                    onPlaylistRemoveClick = { item ->
                                                        uiState =
                                                            uiState.copy(
                                                                playlists = uiState.playlists.filterNot { p -> p.id == item.id },
                                                                selectedPlaylist =
                                                                    uiState.selectedPlaylist?.takeUnless { p ->
                                                                        p.id == item.id
                                                                    },
                                                            )
                                                    },
                                                    onOpenSetupGuideClick = {},
                                                    onOpenSetupClick = { navigateToRoute(SetupRoute) },
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            } else {
                                                PlaylistDetail(
                                                    playlist = playlist,
                                                    onEntryClick = onEntryClick,
                                                    showTitle = false,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                        }
                                    }
                                }
                                composable<SetupRoute> {
                                    var previewSetupStep by rememberSaveable { mutableStateOf(SetupStep.Frontends) }
                                    BackHandler(enabled = previewSetupStep != SetupStep.Frontends) {
                                        previewSetupStep = SetupStep.Frontends
                                    }
                                    if (useTwoPane) {
                                        SetupContent(
                                            setupFrontends = previewSetupFrontends,
                                            setupStep = previewSetupStep,
                                            setupState = previewSetupState,
                                            useTwoPane = true,
                                            onFrontendClick = { frontendId ->
                                                if (frontendId == SETUP_FRONTEND_ES_DE) {
                                                    previewSetupStep = SetupStep.EsDe
                                                }
                                            },
                                            onChooseEsDeFolderClick = {},
                                            onPresetSelectedChange = { _, _ -> },
                                            onSaveClick = {},
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        AnimatedContent(
                                            targetState = previewSetupStep,
                                            modifier = Modifier.weight(1f),
                                            transitionSpec = { fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS)) },
                                            label = "previewSetupSinglePaneContent",
                                        ) { targetSetupStep ->
                                            SetupContent(
                                                setupFrontends = previewSetupFrontends,
                                                setupStep = targetSetupStep,
                                                setupState = previewSetupState,
                                                useTwoPane = false,
                                                onFrontendClick = { frontendId ->
                                                    if (frontendId == SETUP_FRONTEND_ES_DE) {
                                                        previewSetupStep = SetupStep.EsDe
                                                    }
                                                },
                                                onChooseEsDeFolderClick = {},
                                                onPresetSelectedChange = { _, _ -> },
                                                onSaveClick = {},
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                                composable<HelpRoute> {
                                    HelpContent(
                                        onOpenSetupGuideClick = {},
                                        onOpenSetupClick = { navigateToRoute(SetupRoute) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
