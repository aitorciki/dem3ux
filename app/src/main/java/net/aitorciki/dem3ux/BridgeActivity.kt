package net.aitorciki.dem3ux

import android.content.ComponentName
import android.content.Intent
import net.aitorciki.dem3ux.bridge.BridgeContract
import net.aitorciki.dem3ux.bridge.BridgeLaunch

class BridgeActivity : BaseBridgeActivity() {
    override fun createBridgeLaunch(sourceIntent: Intent): BridgeLaunch? {
        val targetActivity = sourceIntent.getStringExtra(BridgeContract.EXTRA_TARGET_ACTIVITY)
        val inputPath = sourceIntent.getStringExtra(BridgeContract.EXTRA_INPUT_PATH)
        val targetComponent = targetActivity?.let(ComponentName::unflattenFromString)

        if (targetComponent == null || inputPath.isNullOrBlank()) {
            return null
        }

        return BridgeLaunch(
            inputPath = inputPath,
            targetComponents = listOf(targetComponent),
            targetAction = sourceIntent.action,
        )
    }
}
