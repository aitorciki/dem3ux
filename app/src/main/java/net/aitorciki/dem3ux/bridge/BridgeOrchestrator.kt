package net.aitorciki.dem3ux.bridge

import android.content.ActivityNotFoundException
import android.content.Intent
import net.aitorciki.dem3ux.data.PlaylistRepository

class BridgeOrchestrator(
    private val playlistRepository: PlaylistRepository,
    private val playlistContentReader: PlaylistContentReader,
    private val persistedTreeUrisProvider: PersistedTreeUrisProvider,
    private val logger: (String, Throwable?) -> Unit,
) {
    suspend fun runBridge(
        sourceIntent: Intent,
        bridgeLaunch: BridgeLaunch,
        targetLauncher: TargetLauncher,
    ): BridgeOutcome {
        val inputPath = bridgeLaunch.inputPath
        val isPlaylist = inputPath.isPlaylist()

        val selectedEntry =
            if (isPlaylist) {
                when (val playlistContentResult = playlistContentReader.read(inputPath.raw)) {
                    is PlaylistContentResult.Success -> {
                        runCatching {
                            playlistRepository
                                .recordSeenPlaylist(sourcePath = inputPath.raw, content = playlistContentResult.content)
                        }.onFailure { error ->
                            logger("Failed to record playlist", error)
                        }.getOrNull()
                            ?.selectedEntryPath
                            ?.let(::SelectedEntryPath)
                    }

                    is PlaylistContentResult.NeedsPermission -> {
                        return BridgeOutcome.NeedsFolderAccess(bridgeLaunch)
                    }

                    is PlaylistContentResult.Failed -> {
                        return BridgeOutcome.Failed("Failed to read playlist content", playlistContentResult.error)
                    }
                }
            } else {
                SelectedEntryPath(inputPath.raw)
            }

        if (selectedEntry == null || selectedEntry.raw.isBlank()) {
            return BridgeOutcome.Failed("Finishing bridge launch because no selected entry was resolved")
        }

        val persistedTreeUris = persistedTreeUrisProvider.persistedReadableTreeUris()

        if (selectedEntry.requiresFolderAccessForForwarding(persistedTreeUris)) {
            return BridgeOutcome.NeedsFolderAccess(bridgeLaunch)
        }

        val grantableSelectedEntry = selectedEntry.mapContentUriThroughPersistedTreeGrant(persistedTreeUris)

        return launchTargetEmulator(
            sourceIntent = sourceIntent,
            bridgeLaunch = bridgeLaunch,
            inputPath = inputPath,
            selectedEntry = grantableSelectedEntry,
            targetLauncher = targetLauncher,
        )
    }

    private suspend fun launchTargetEmulator(
        sourceIntent: Intent,
        bridgeLaunch: BridgeLaunch,
        inputPath: BridgeInputPath,
        selectedEntry: SelectedEntryPath,
        targetLauncher: TargetLauncher,
    ): BridgeOutcome {
        var lastActivityNotFound: ActivityNotFoundException? = null

        bridgeLaunch.targetComponents.forEach { targetComponent ->
            val targetIntent =
                BridgeTargetIntentFactory.build(
                    sourceIntent = sourceIntent,
                    targetComponent = targetComponent,
                    targetAction = bridgeLaunch.targetAction,
                    inputPath = inputPath,
                    selectedEntry = selectedEntry,
                    embeddedExtraReplacement = bridgeLaunch.embeddedExtraReplacement,
                )

            when (val result = targetLauncher.launch(targetIntent)) {
                TargetLaunchResult.Success -> return BridgeOutcome.Launched
                is TargetLaunchResult.ActivityNotFound -> lastActivityNotFound = result.error
                is TargetLaunchResult.Error -> return BridgeOutcome.Failed("Failed to launch target emulator.", result.error)
            }
        }

        return BridgeOutcome.Failed("Failed to launch target emulator.", lastActivityNotFound)
    }
}
