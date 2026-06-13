package net.aitorciki.dem3ux

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
import net.aitorciki.dem3ux.bridge.ExternalStorageUriMapper
import net.aitorciki.dem3ux.data.Dem3uxDatabaseProvider
import net.aitorciki.dem3ux.data.PlaylistRepository
import java.io.File

abstract class BaseBridgeActivity : ComponentActivity() {
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
                            PlaylistRepository(Dem3uxDatabaseProvider.get(this@BaseBridgeActivity))
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

            val grantableSelectedEntry = selectedEntry.mapThroughPersistedTreeGrant()

            val targetIntent =
                BridgeTargetIntentFactory.build(
                    sourceIntent = intent,
                    targetComponent = bridgeLaunch.targetComponent,
                    targetAction = bridgeLaunch.targetAction,
                    inputPath = inputPath,
                    selectedEntry = grantableSelectedEntry,
                )
            runCatching {
                startActivity(targetIntent)
            }.onFailure { error ->
                logBridgeFailure("Failed to launch target emulator.", error)
            }
            finish()
        }
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
                        File(inputPath).readText()
                    }
                }
            }.onFailure { error ->
                logBridgeFailure("Failed to read playlist content", error)
            }.getOrElse { error ->
                return PlaylistContentResult(securityException = error as? SecurityException)
            }

        return PlaylistContentResult(content = content)
    }

    private fun String.mapThroughPersistedTreeGrant(): String =
        ExternalStorageUriMapper.mapToPersistedTreeUri(
            uriString = this,
            persistedTreeUris =
                persistedReadableTreeUris(),
        ) ?: this

    private fun String.requiresFolderAccessForForwarding(): Boolean =
        ExternalStorageUriMapper.documentId(this) != null &&
            !ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString = this,
                persistedTreeUris = persistedReadableTreeUris(),
            )

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
        val targetComponent: ComponentName,
        val targetAction: String? = null,
        val requestedFolderAccess: Boolean = false,
    )

    private data class PlaylistContentResult(
        val content: String? = null,
        val securityException: SecurityException? = null,
    )

    private companion object {
        const val TAG = "dem3ux"
    }
}
