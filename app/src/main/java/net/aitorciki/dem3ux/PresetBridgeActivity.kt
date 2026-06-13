package net.aitorciki.dem3ux

import android.content.Intent
import net.aitorciki.dem3ux.bridge.PresetBridges

class PresetBridgeActivity : BaseBridgeActivity() {
    override fun createBridgeLaunch(sourceIntent: Intent): BridgeLaunch? {
        val preset = PresetBridges.fromAliasClassName(componentName.className) ?: PresetBridges.duckStation
        val inputPath = preset.inputPathFrom(sourceIntent)
        val targetComponent = preset.targetComponent

        if (targetComponent == null || inputPath.isNullOrBlank()) {
            return null
        }

        return BridgeLaunch(
            inputPath = inputPath,
            targetComponent = targetComponent,
            targetAction = preset.targetAction,
        )
    }
}
