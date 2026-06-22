package net.aitorciki.dem3ux

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.bridge.BridgeInputType
import net.aitorciki.dem3ux.bridge.BridgeTargetIntentFactory
import net.aitorciki.dem3ux.bridge.EmbeddedExtraPattern
import net.aitorciki.dem3ux.bridge.ExternalStorageUriMapper
import net.aitorciki.dem3ux.data.PlaylistRepository
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileNotFoundException

abstract class BaseBridgeActivity :
    ComponentActivity(),
    KoinComponent {
    private val playlistRepository: PlaylistRepository by inject()
    private var pendingBridgeLaunch: BridgeLaunch? = null

    private val openTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val pendingLaunch = pendingBridgeLaunch
            pendingBridgeLaunch = null

            if (uri == null || pendingLaunch == null) {
                logBridgeFailure("Folder access request was cancelled")
                finish()
                return@registerForActivityResult
            }

            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onFailure { error ->
                logBridgeFailure("Failed to persist folder access", error)
            }

            Log.i(TAG, "Retrying bridge launch after folder access grant")
            runBridge(pendingLaunch)
        }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bridgeLaunch = createBridgeLaunch(intent)
        if (bridgeLaunch == null) {
            logBridgeFailure("Finishing bridge launch because target or input is missing")
            finish()
            return
        }

        runBridge(bridgeLaunch)
    }

    protected abstract fun createBridgeLaunch(sourceIntent: Intent): BridgeLaunch?

    private fun runBridge(bridgeLaunch: BridgeLaunch) {
        lifecycleScope.launch {
            val inputPath = bridgeLaunch.inputPath
            val isPlaylist = BridgeInputType.isPlaylist(inputPath)

            val selectedEntry =
                if (isPlaylist) {
                    val playlistContentResult = readPlaylistContent(inputPath)
                    if (playlistContentResult.securityException != null) {
                        requestFolderAccessAndRetry(bridgeLaunch)
                        return@launch
                    }

                    val playlistContent = playlistContentResult.content

                    playlistContent?.let { content ->
                        runCatching {
                            playlistRepository
                                .recordSeenPlaylist(sourcePath = inputPath, content = content)
                        }.onFailure { error ->
                            logBridgeFailure("Failed to record playlist", error)
                        }.getOrNull()
                            ?.selectedEntryPath
                    }
                } else {
                    inputPath
                }

            if (selectedEntry.isNullOrBlank()) {
                logBridgeFailure("Finishing bridge launch because no selected entry was resolved")
                finish()
                return@launch
            }

            if (selectedEntry.requiresFolderAccessForForwarding()) {
                requestFolderAccessAndRetry(bridgeLaunch)
                return@launch
            }

            val grantableSelectedEntry = selectedEntry.mapContentUriThroughPersistedTreeGrant()

            launchTargetEmulator(
                bridgeLaunch = bridgeLaunch,
                inputPath = inputPath,
                selectedEntry = grantableSelectedEntry,
            )
            finish()
        }
    }

    private fun launchTargetEmulator(
        bridgeLaunch: BridgeLaunch,
        inputPath: String,
        selectedEntry: String,
    ) {
        var lastActivityNotFound: ActivityNotFoundException? = null

        bridgeLaunch.targetComponents.forEach { targetComponent ->
            val targetIntent =
                BridgeTargetIntentFactory.build(
                    sourceIntent = intent,
                    targetComponent = targetComponent,
                    targetAction = bridgeLaunch.targetAction,
                    inputPath = inputPath,
                    selectedEntry = selectedEntry,
                    embeddedExtraReplacement = bridgeLaunch.embeddedExtraReplacement,
                )

            try {
                startActivity(targetIntent)
                return
            } catch (error: ActivityNotFoundException) {
                lastActivityNotFound = error
            } catch (error: Throwable) {
                logBridgeFailure("Failed to launch target emulator.", error)
                return
            }
        }

        logBridgeFailure("Failed to launch target emulator.", lastActivityNotFound)
    }

    private fun requestFolderAccessAndRetry(bridgeLaunch: BridgeLaunch) {
        if (bridgeLaunch.requestedFolderAccess) {
            logBridgeFailure("Folder access was already requested, but the input is still inaccessible.")
            Toast
                .makeText(
                    this,
                    "dem3ux still cannot access this game. Select the ROMs folder that contains it.",
                    Toast.LENGTH_LONG,
                ).show()
            finish()
            return
        }

        pendingBridgeLaunch = bridgeLaunch.copy(requestedFolderAccess = true)
        Toast.makeText(this, "Select the ROMs folder so dem3ux can access this game.", Toast.LENGTH_LONG).show()
        openTreeLauncher.launch(null)
    }

    private fun readPlaylistContent(inputPath: String): PlaylistContentResult {
        val content =
            runCatching {
                when {
                    inputPath.startsWith("content://") -> {
                        val readableInputPath = inputPath.mapThroughPersistedTreeGrant()
                        contentResolver.openInputStream(readableInputPath.toUri())?.bufferedReader()?.use { reader ->
                            reader.readText()
                        }
                    }

                    inputPath.startsWith("file://") -> {
                        File(requireNotNull(inputPath.toUri().path)).readText()
                    }

                    else -> {
                        val readableInputPath = inputPath.mapThroughPersistedTreeGrant()
                        if (readableInputPath != inputPath) {
                            contentResolver.openInputStream(readableInputPath.toUri())?.bufferedReader()?.use { reader ->
                                reader.readText()
                            }
                        } else {
                            File(inputPath).readText()
                        }
                    }
                }
            }.onFailure { error ->
                logBridgeFailure("Failed to read playlist content", error)
            }.getOrElse { error ->
                return PlaylistContentResult(securityException = error.asPermissionException())
            }

        return PlaylistContentResult(content = content)
    }

    private fun String.mapThroughPersistedTreeGrant(): String =
        ExternalStorageUriMapper.mapToPersistedTreeUri(
            uriString = this,
            persistedTreeUris =
                persistedReadableTreeUris(),
        ) ?: this

    private fun String.mapContentUriThroughPersistedTreeGrant(): String =
        if (ExternalStorageUriMapper.documentId(this) != null) {
            mapThroughPersistedTreeGrant()
        } else {
            this
        }

    private fun String.requiresFolderAccessForForwarding(): Boolean =
        ExternalStorageUriMapper.documentId(this) != null &&
            !ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString = this,
                persistedTreeUris = persistedReadableTreeUris(),
            )

    private fun Throwable.asPermissionException(): SecurityException? =
        when {
            this is SecurityException -> this
            this is FileNotFoundException && message?.contains("EACCES") == true -> SecurityException(message, this)
            else -> null
        }

    private fun persistedReadableTreeUris(): List<String> =
        contentResolver.persistedUriPermissions
            .filter { permission -> permission.isReadPermission }
            .map { permission -> permission.uri.toString() }

    private fun logBridgeFailure(
        message: String,
        error: Throwable? = null,
    ) {
        if (error == null) {
            Log.w(TAG, message)
        } else {
            Log.w(TAG, message, error)
        }
    }

    protected data class BridgeLaunch(
        val inputPath: String,
        val targetComponents: List<ComponentName>,
        val targetAction: String? = null,
        val embeddedExtraReplacement: EmbeddedExtraPattern? = null,
        val requestedFolderAccess: Boolean = false,
    ) {
        constructor(
            inputPath: String,
            targetComponent: ComponentName,
            targetAction: String? = null,
            embeddedExtraReplacement: EmbeddedExtraPattern? = null,
            requestedFolderAccess: Boolean = false,
        ) : this(
            inputPath = inputPath,
            targetComponents = listOf(targetComponent),
            targetAction = targetAction,
            embeddedExtraReplacement = embeddedExtraReplacement,
            requestedFolderAccess = requestedFolderAccess,
        )
    }

    private data class PlaylistContentResult(
        val content: String? = null,
        val securityException: SecurityException? = null,
    )

    private companion object {
        const val TAG = "dem3ux"
    }
}
