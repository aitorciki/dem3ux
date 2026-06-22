package net.aitorciki.dem3ux

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.bridge.BridgeLaunch
import net.aitorciki.dem3ux.bridge.BridgeOrchestrator
import net.aitorciki.dem3ux.bridge.BridgeOutcome
import net.aitorciki.dem3ux.bridge.TargetLaunchResult
import net.aitorciki.dem3ux.bridge.TargetLauncher
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

abstract class BaseBridgeActivity :
    ComponentActivity(),
    TargetLauncher,
    KoinComponent {
    private val bridgeOrchestrator: BridgeOrchestrator by inject()
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

    override fun launch(targetIntent: Intent): TargetLaunchResult =
        try {
            startActivity(targetIntent)
            TargetLaunchResult.Success
        } catch (error: ActivityNotFoundException) {
            TargetLaunchResult.ActivityNotFound(error)
        } catch (error: Throwable) {
            TargetLaunchResult.Error(error)
        }

    private fun runBridge(bridgeLaunch: BridgeLaunch) {
        lifecycleScope.launch {
            val outcome = bridgeOrchestrator.runBridge(intent, bridgeLaunch, this@BaseBridgeActivity)
            handleOutcome(outcome)
        }
    }

    private fun handleOutcome(outcome: BridgeOutcome) {
        when (outcome) {
            BridgeOutcome.Launched -> {
                finish()
            }

            is BridgeOutcome.NeedsFolderAccess -> {
                requestFolderAccessAndRetry(outcome.bridgeLaunch)
            }

            is BridgeOutcome.Failed -> {
                logBridgeFailure(outcome.message, outcome.error)
                finish()
            }
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

    private companion object {
        const val TAG = "dem3ux"
    }
}
