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
        val isPlaylist = BridgeInputType.isPlaylist(inputPath)

        val selectedEntry =
            if (isPlaylist) {
                val playlistContentResult = playlistContentReader.read(inputPath)
                if (playlistContentResult.securityException != null) {
                    return BridgeOutcome.NeedsFolderAccess(bridgeLaunch)
                }

                val playlistContent = playlistContentResult.content

                playlistContent?.let { content ->
                    runCatching {
                        playlistRepository
                            .recordSeenPlaylist(sourcePath = inputPath, content = content)
                    }.onFailure { error ->
                        logger("Failed to record playlist", error)
                    }.getOrNull()
                        ?.selectedEntryPath
                }
            } else {
                inputPath
            }

        if (selectedEntry.isNullOrBlank()) {
            return BridgeOutcome.Failed("Finishing bridge launch because no selected entry was resolved")
        }

        if (selectedEntry.requiresFolderAccessForForwarding()) {
            return BridgeOutcome.NeedsFolderAccess(bridgeLaunch)
        }

        val grantableSelectedEntry = selectedEntry.mapContentUriThroughPersistedTreeGrant()

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
        inputPath: String,
        selectedEntry: String,
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

    private fun String.requiresFolderAccessForForwarding(): Boolean =
        ExternalStorageUriMapper.documentId(this) != null &&
            !ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString = this,
                persistedTreeUris = persistedTreeUrisProvider.persistedReadableTreeUris(),
            )

    private fun String.mapContentUriThroughPersistedTreeGrant(): String =
        if (ExternalStorageUriMapper.documentId(this) != null) {
            mapThroughPersistedTreeGrant()
        } else {
            this
        }

    private fun String.mapThroughPersistedTreeGrant(): String =
        ExternalStorageUriMapper.mapToPersistedTreeUri(
            uriString = this,
            persistedTreeUris = persistedTreeUrisProvider.persistedReadableTreeUris(),
        ) ?: this
}
