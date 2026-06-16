package net.aitorciki.dem3ux

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.ui.Dem3uxUiState
import net.aitorciki.dem3ux.ui.Dem3uxViewModel
import net.aitorciki.dem3ux.ui.EsDeSetupPresetUi
import net.aitorciki.dem3ux.ui.EsDeSetupUiState
import net.aitorciki.dem3ux.ui.PlaylistDetailUi
import net.aitorciki.dem3ux.ui.PlaylistEntryUi
import net.aitorciki.dem3ux.ui.PlaylistSummaryUi

private const val SETUP_GUIDE_URL = "https://github.com/aitorciki/dem3ux#frontend-integration"

private enum class MainDestination {
    Playlists,
    Setup,
    Help,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Dem3uxApp()
        }
    }
}

@Composable
fun Dem3uxApp() {
    val viewModel: Dem3uxViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Dem3uxApp(
        uiState = uiState,
        versionLabel = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        onPlaylistClick = viewModel::selectPlaylist,
        onBackClick = viewModel::clearSelectedPlaylist,
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
    var destination by remember { mutableStateOf(MainDestination.Playlists) }
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
            val openDrawer: () -> Unit = {
                coroutineScope.launch { drawerState.open() }
            }
            val openSetupGuide: () -> Unit = {
                uriHandler.openUri(SETUP_GUIDE_URL)
            }
            val openSetup: () -> Unit = {
                destination = MainDestination.Setup
            }

            BackHandler(enabled = destination == MainDestination.Help || destination == MainDestination.Setup) {
                destination = MainDestination.Playlists
            }
            BackHandler(enabled = destination == MainDestination.Playlists && !useTwoPane && selectedPlaylist != null) {
                onBackClick()
            }
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Dem3uxDrawer(
                        destination = destination,
                        versionLabel = versionLabel,
                        onDestinationClick = { selectedDestination ->
                            destination = selectedDestination
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
                        if (destination == MainDestination.Playlists) {
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
                                    setupState = uiState.esDeSetup,
                                    onChooseEsDeFolderClick = { openEsDeFolderLauncher.launch(openDocumentTreeIntent()) },
                                    onPresetSelectedChange = onEsDePresetSelectedChange,
                                    onSaveClick = onSaveEsDeSetupClick,
                                    modifier = Modifier.weight(1f),
                                )
                            } else if (useTwoPane) {
                                TwoPaneContent(
                                    uiState = uiState,
                                    onPlaylistClick = onPlaylistClick,
                                    onEntryClick = onEntryClick,
                                    onOpenSetupGuideClick = openSetupGuide,
                                    onOpenSetupClick = openSetup,
                                    modifier = Modifier.weight(1f),
                                )
                            } else if (uiState.selectedPlaylist == null) {
                                PlaylistList(
                                    playlists = uiState.playlists,
                                    onPlaylistClick = onPlaylistClick,
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

private fun openDocumentTreeIntent(): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
        )
    }

@Composable
private fun Dem3uxTheme(content: @Composable () -> Unit) {
    val colorScheme =
        if (isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
private fun TwoPaneContent(
    uiState: Dem3uxUiState,
    onPlaylistClick: (Long) -> Unit,
    onEntryClick: (Long, Int) -> Unit,
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        PlaylistList(
            playlists = uiState.playlists,
            onPlaylistClick = onPlaylistClick,
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaylistDetailTopBar(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.basicMarquee(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                )
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onMenuClick: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Column {
                Text(text = title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = "Open navigation drawer",
                    )
                }
            }
        },
    )
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

@Composable
private fun SetupContent(
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
                items(installedPresets, key = { preset -> preset.id }) { preset ->
                    EsDePresetRow(
                        preset = preset,
                        enabled = setupState.hasFolderAccess,
                        onSelectedChange = { selected -> onPresetSelectedChange(preset.id, selected) },
                    )
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
    onSelectedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.alpha(if (enabled) 1f else 0.56f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onSelectedChange(!preset.selected) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
            Checkbox(
                checked = preset.selected,
                onCheckedChange = onSelectedChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<PlaylistSummaryUi>,
    onPlaylistClick: (Long) -> Unit,
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            EmptyPlaylistList(
                onOpenSetupGuideClick = onOpenSetupGuideClick,
                onOpenSetupClick = onOpenSetupClick,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(playlists, key = { playlist -> playlist.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistSummaryUi,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = playlist.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                text = "Selected: ${playlist.selectedEntryName}",
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

@Composable
private fun PlaylistDetail(
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(playlist.entries, key = { entry -> entry.index }) { entry ->
                PlaylistEntryRow(
                    entry = entry,
                    onClick = { onEntryClick(playlist.id, entry.index) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistEntryRow(
    entry: PlaylistEntryUi,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (entry.selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            RadioButton(
                selected = entry.selected,
                onClick = onClick,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.rawLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistList(
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "No playlists yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SetupGuideText(
                onOpenSetupGuideClick = onOpenSetupGuideClick,
                onOpenSetupClick = onOpenSetupClick,
            )
        }
    }
}

@Composable
private fun HelpContent(
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Setup guide",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                SetupGuideText(
                    onOpenSetupGuideClick = onOpenSetupGuideClick,
                    onOpenSetupClick = onOpenSetupClick,
                )
            }
        }
    }
}

@Composable
private fun SetupGuideText(
    onOpenSetupGuideClick: () -> Unit,
    onOpenSetupClick: () -> Unit,
) {
    Text(
        text = "dem3ux is a bridge and must be configured in your emulator frontend.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            buildAnnotatedString {
                append("If your frontend is supported, you can configure it directly from the ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "setup",
                        styles =
                            TextLinkStyles(
                                style =
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                            ),
                        linkInteractionListener = { onOpenSetupClick() },
                    ),
                ) {
                    append("Setup")
                }
                append(" section.")
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text =
            buildAnnotatedString {
                append("Otherwise, configure the frontend manually to launch ")
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append("net.aitorciki.dem3ux/.BridgeActivity")
                }
                append(" and pass the target emulator with ")
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append("dem3ux.target.activity")
                }
                append(".")
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Setup examples are available in the project README.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(
        onClick = onOpenSetupGuideClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text("Open setup guide")
    }
}

@Composable
private fun EmptyDetail(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Select a playlist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Entries will appear here with the current default marked.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

@Preview(showBackground = true, name = "Playlist list")
@Composable
private fun Dem3uxAppListPreview() {
    Dem3uxApp(
        uiState = previewListState,
        versionLabel = PREVIEW_VERSION_LABEL,
        onPlaylistClick = {},
        onBackClick = {},
        onEntryClick = { _, _ -> },
        onOpenPlaylistClick = {},
        onEsDeFolderSelected = { _, _ -> },
        onEsDePresetSelectedChange = { _, _ -> },
        onSaveEsDeSetupClick = {},
        onImportMessageShown = {},
    )
}

@Preview(showBackground = true, name = "Playlist detail")
@Composable
private fun Dem3uxAppDetailPreview() {
    Dem3uxApp(
        uiState = previewDetailState,
        versionLabel = PREVIEW_VERSION_LABEL,
        onPlaylistClick = {},
        onBackClick = {},
        onEntryClick = { _, _ -> },
        onOpenPlaylistClick = {},
        onEsDeFolderSelected = { _, _ -> },
        onEsDePresetSelectedChange = { _, _ -> },
        onSaveEsDeSetupClick = {},
        onImportMessageShown = {},
    )
}

@Preview(showBackground = true, name = "Help")
@Composable
private fun HelpContentPreview() {
    Dem3uxTheme {
        PreviewScreenFrame {
            HelpContent(
                onOpenSetupGuideClick = {},
                onOpenSetupClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Setup")
@Composable
private fun SetupContentPreview() {
    Dem3uxTheme {
        PreviewScreenFrame {
            SetupContent(
                setupState = previewSetupState,
                onChooseEsDeFolderClick = {},
                onPresetSelectedChange = { _, _ -> },
                onSaveClick = {},
            )
        }
    }
}

@Composable
private fun PreviewScreenFrame(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            content()
        }
    }
}

private val previewPlaylists =
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

private val previewNebulaDriftDetail =
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

private val previewArcadeSamplerDetail =
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

private val previewDetailsById =
    mapOf(
        previewNebulaDriftDetail.id to previewNebulaDriftDetail,
        previewArcadeSamplerDetail.id to previewArcadeSamplerDetail,
    )

private val previewListState =
    Dem3uxUiState(
        playlists = previewPlaylists,
    )

private val previewDetailState =
    Dem3uxUiState(
        playlists = previewPlaylists,
        selectedPlaylist = previewNebulaDriftDetail,
    )

private val previewSetupState =
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

private const val PREVIEW_VERSION_LABEL = "Version 1.0.0 (1)"
