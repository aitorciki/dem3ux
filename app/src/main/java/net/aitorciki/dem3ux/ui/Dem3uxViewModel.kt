package net.aitorciki.dem3ux.ui

import android.app.Application
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.R
import net.aitorciki.dem3ux.bridge.PlaylistContentReader
import net.aitorciki.dem3ux.bridge.PlaylistContentResult
import net.aitorciki.dem3ux.bridge.PresetBridge
import net.aitorciki.dem3ux.bridge.PresetBridges
import net.aitorciki.dem3ux.data.PlaylistRepository
import net.aitorciki.dem3ux.data.PlaylistWithEntries
import net.aitorciki.dem3ux.setup.EsDeFindRuleSelection
import net.aitorciki.dem3ux.setup.EsDeFindRulesEditor
import net.aitorciki.dem3ux.setup.EsDeSetupRepository

class Dem3uxViewModel(
    application: Application,
    private val repository: PlaylistRepository,
    private val esDeSetupRepository: EsDeSetupRepository,
    private val playlistContentReader: PlaylistContentReader,
) : AndroidViewModel(application) {
    private val resources = application.resources
    private val selectedPlaylistId = MutableStateFlow<Long?>(null)
    private val importMessage = MutableStateFlow<String?>(null)
    private val esDeCustomSystemsUri = MutableStateFlow<Uri?>(null)
    private val selectedEsDePresetIds = MutableStateFlow<Set<String>>(emptySet())

    private val esDeSetupState =
        combine(esDeCustomSystemsUri, selectedEsDePresetIds) { uri, selectedPresetIds ->
            buildEsDeSetupUiState(uri = uri, selectedPresetIds = selectedPresetIds)
        }

    val uiState =
        combine(
            repository.observePlaylistsWithEntries(),
            selectedPlaylistId,
            importMessage,
            esDeSetupState,
        ) { playlists, selectedId, message, setupState ->
            val sortedPlaylists = playlists.map { playlist -> playlist.withSortedEntries() }
            val selectedPlaylist = sortedPlaylists.firstOrNull { playlist -> playlist.playlist.id == selectedId }

            Dem3uxUiState(
                playlists = sortedPlaylists.map { playlist -> playlist.toSummaryUi() },
                playlistsLoaded = true,
                selectedPlaylist = selectedPlaylist?.toDetailUi(),
                setupFrontends = buildSetupFrontendsUi(),
                esDeSetup = setupState,
                importMessage = message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Dem3uxUiState(),
        )

    init {
        restoreEsDeSetupFolder()
    }

    fun selectPlaylist(playlistId: Long) {
        selectedPlaylistId.value = playlistId
    }

    fun clearSelectedPlaylist() {
        selectedPlaylistId.value = null
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (selectedPlaylistId.value == playlistId) {
                selectedPlaylistId.value = null
            }
            importMessage.value = resources.getString(R.string.playlist_removed)
        }
    }

    fun selectEntry(
        playlistId: Long,
        entryIndex: Int,
    ) {
        viewModelScope.launch {
            repository.selectEntry(playlistId = playlistId, entryIndex = entryIndex)
        }
    }

    fun importPlaylist(uri: Uri) {
        viewModelScope.launch {
            val result =
                runCatching {
                    val application = getApplication<Application>()
                    application.contentResolver
                        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    when (val readResult = playlistContentReader.read(uri.toString())) {
                        is PlaylistContentResult.Success -> {
                            repository.recordSeenPlaylist(
                                sourcePath = uri.toString(),
                                content = readResult.content,
                            )
                        }

                        is PlaylistContentResult.NeedsPermission,
                        is PlaylistContentResult.Failed,
                        -> {
                            null
                        }
                    }
                }

            val selection = result.getOrNull()
            if (selection == null) {
                importMessage.value = resources.getString(R.string.playlist_import_failed)
            } else {
                selectedPlaylistId.value = selection.playlistId
                importMessage.value = resources.getString(R.string.playlist_added)
            }
        }
    }

    fun clearImportMessage() {
        importMessage.value = null
    }

    fun selectEsDeCustomSystemsFolder(
        uri: Uri,
        grantFlags: Int,
    ) {
        viewModelScope.launch {
            val result =
                runCatching {
                    esDeSetupRepository.persistCustomSystemsFolder(uri = uri, grantFlags = grantFlags)
                    val currentRules = esDeSetupRepository.readFindRules(uri)
                    val selectedEmulatorNames =
                        EsDeFindRulesEditor.selectedEmulatorNames(
                            currentRules,
                            esDeRuleSelections(selected = true),
                        )

                    uri to selectedEmulatorNames.toPresetIds()
                }

            result
                .onSuccess { (folderUri, selectedPresetIds) ->
                    esDeCustomSystemsUri.value = folderUri
                    selectedEsDePresetIds.value = selectedPresetIds
                    importMessage.value = resources.getString(R.string.es_de_folder_selected)
                }.onFailure { error ->
                    Log.w(TAG, "Could not open ES-DE custom_systems folder.", error)
                    importMessage.value = resources.getString(R.string.es_de_folder_open_failed)
                }
        }
    }

    private fun restoreEsDeSetupFolder() {
        viewModelScope.launch {
            val result =
                runCatching {
                    val uri = esDeSetupRepository.persistedCustomSystemsFolder() ?: return@runCatching null
                    val currentRules = esDeSetupRepository.readFindRules(uri)
                    val selectedEmulatorNames =
                        EsDeFindRulesEditor.selectedEmulatorNames(
                            currentRules,
                            esDeRuleSelections(selected = true),
                        )

                    uri to selectedEmulatorNames.toPresetIds()
                }

            result
                .onSuccess { restoredSetup ->
                    if (restoredSetup != null) {
                        val (folderUri, selectedPresetIds) = restoredSetup
                        esDeCustomSystemsUri.value = folderUri
                        selectedEsDePresetIds.value = selectedPresetIds
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Could not restore ES-DE custom_systems folder.", error)
                }
        }
    }

    fun setEsDePresetSelected(
        presetId: String,
        selected: Boolean,
    ) {
        selectedEsDePresetIds.value =
            if (selected) {
                selectedEsDePresetIds.value + presetId
            } else {
                selectedEsDePresetIds.value - presetId
            }
    }

    fun saveEsDeSetup() {
        val folderUri = esDeCustomSystemsUri.value
        if (folderUri == null) {
            importMessage.value = resources.getString(R.string.es_de_folder_required)
            return
        }

        viewModelScope.launch {
            val result =
                runCatching {
                    val currentRules = esDeSetupRepository.readFindRules(folderUri)
                    val updatedRules =
                        EsDeFindRulesEditor.applySelections(
                            inputXml = currentRules,
                            selections = esDeRuleSelections(selectedPresetIds = selectedEsDePresetIds.value),
                        )
                    esDeSetupRepository.saveFindRules(treeUri = folderUri, content = updatedRules)
                }

            importMessage.value =
                if (result.isSuccess) {
                    resources.getString(R.string.es_de_setup_saved)
                } else {
                    resources.getString(R.string.es_de_setup_save_failed)
                }
        }
    }

    private fun PlaylistWithEntries.withSortedEntries(): PlaylistWithEntries =
        copy(entries = entries.sortedBy { entry -> entry.entryIndex })

    private fun PlaylistWithEntries.toSummaryUi(): PlaylistSummaryUi {
        val selectedEntry = entries.firstOrNull { entry -> entry.entryIndex == playlist.selectedEntryIndex } ?: entries.firstOrNull()

        return PlaylistSummaryUi(
            id = playlist.id,
            displayName = playlist.displayName,
            sourcePath = playlist.sourcePath,
            selectedEntryName = selectedEntry?.displayName ?: resources.getString(R.string.no_entries),
            lastSeenLabel =
                resources.getString(
                    R.string.playlist_last_accessed,
                    playlist.lastSeenAt.toRelativeLabel(resources),
                ),
        )
    }

    private fun PlaylistWithEntries.toDetailUi(): PlaylistDetailUi {
        val selectedIndex = playlist.selectedEntryIndex ?: entries.firstOrNull()?.entryIndex

        return PlaylistDetailUi(
            id = playlist.id,
            displayName = playlist.displayName,
            sourcePath = playlist.sourcePath,
            sourcePathLabel = SourcePathLabel.format(playlist.sourcePath),
            entries =
                entries.map { entry ->
                    PlaylistEntryUi(
                        index = entry.entryIndex,
                        displayName = entry.displayName,
                        rawLine = entry.rawLine,
                        resolvedPath = entry.resolvedPath,
                        selected = entry.entryIndex == selectedIndex,
                    )
                },
        )
    }

    private fun buildEsDeSetupUiState(
        uri: Uri?,
        selectedPresetIds: Set<String>,
    ): EsDeSetupUiState =
        EsDeSetupUiState(
            customSystemsUri = uri?.toString(),
            presets =
                PresetBridges.all
                    .filter { preset -> preset.esDeEmulatorName != null }
                    .map { preset -> preset.toSetupPresetUi(selected = preset.id in selectedPresetIds) },
        )

    private fun buildSetupFrontendsUi(): List<SetupFrontendUi> {
        val installedFrontend = esDeSetupRepository.installedFrontend()

        return listOf(
            SetupFrontendUi(
                id = SETUP_FRONTEND_ES_DE,
                displayName = resources.getString(R.string.frontend_es_de_name),
                description = resources.getString(R.string.frontend_es_de_description),
                installed = installedFrontend != null,
                installedIcon = installedFrontend?.icon,
            ),
        )
    }

    private fun PresetBridge.toSetupPresetUi(selected: Boolean): EsDeSetupPresetUi {
        val installedTarget = esDeSetupRepository.installedPresetTarget(this)

        return EsDeSetupPresetUi(
            id = id,
            displayName = displayName,
            esDeEmulatorName = requireNotNull(esDeEmulatorName),
            aliasEntry = esDeAliasEntry,
            installed = installedTarget != null,
            installedTargetIcon = installedTarget?.icon,
            selected = selected,
        )
    }

    private fun esDeRuleSelections(selected: Boolean): List<EsDeFindRuleSelection> =
        esDePresets().map { preset -> preset.toFindRuleSelection(selected = selected) }

    private fun esDeRuleSelections(selectedPresetIds: Set<String>): List<EsDeFindRuleSelection> =
        esDePresets().map { preset -> preset.toFindRuleSelection(selected = preset.id in selectedPresetIds) }

    private fun PresetBridge.toFindRuleSelection(selected: Boolean): EsDeFindRuleSelection =
        EsDeFindRuleSelection(
            emulatorName = requireNotNull(esDeEmulatorName),
            aliasEntry = esDeAliasEntry,
            selected = selected,
        )

    private fun Set<String>.toPresetIds(): Set<String> {
        val emulatorNameToPresetId = esDePresets().associate { preset -> requireNotNull(preset.esDeEmulatorName) to preset.id }
        return mapNotNull(emulatorNameToPresetId::get).toSet()
    }

    private fun esDePresets(): List<PresetBridge> = PresetBridges.all.filter { preset -> preset.esDeEmulatorName != null }
}

private fun Long.toRelativeLabel(resources: Resources): String {
    val elapsedMillis = (System.currentTimeMillis() - this).coerceAtLeast(0)
    val elapsedMinutes = elapsedMillis / 60_000
    val elapsedHours = elapsedMinutes / 60
    val elapsedDays = elapsedHours / 24

    return when {
        elapsedMinutes < 1 -> {
            resources.getString(R.string.relative_just_now)
        }

        elapsedMinutes == 1L -> {
            resources.getString(R.string.relative_one_minute_ago)
        }

        elapsedMinutes < 60 -> {
            resources.getQuantityString(
                R.plurals.relative_minutes_ago,
                elapsedMinutes.toInt(),
                elapsedMinutes.toInt(),
            )
        }

        elapsedHours == 1L -> {
            resources.getString(R.string.relative_one_hour_ago)
        }

        elapsedHours < 24 -> {
            resources.getQuantityString(
                R.plurals.relative_hours_ago,
                elapsedHours.toInt(),
                elapsedHours.toInt(),
            )
        }

        elapsedDays == 1L -> {
            resources.getString(R.string.relative_yesterday)
        }

        else -> {
            resources.getQuantityString(
                R.plurals.relative_days_ago,
                elapsedDays.toInt(),
                elapsedDays.toInt(),
            )
        }
    }
}

private const val TAG = "dem3ux"
const val SETUP_FRONTEND_ES_DE = "es_de"
