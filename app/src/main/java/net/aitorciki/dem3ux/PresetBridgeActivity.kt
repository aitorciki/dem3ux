package net.aitorciki.dem3ux

import android.content.Intent
import net.aitorciki.dem3ux.bridge.PresetBridges

class PresetBridgeActivity : BaseBridgeActivity() {
    override fun createBridgeLaunch(sourceIntent: Intent): BridgeLaunch? {
        val aliasClassName = sourceIntent.component?.className ?: componentName.className
        val preset = PresetBridges.fromAliasClassName(aliasClassName) ?: PresetBridges.duckStation
        val inputPath = preset.inputPathFrom(sourceIntent)
        val targetComponents = preset.targetComponents

        if (targetComponents.isEmpty() || inputPath.isNullOrBlank()) {
            return null
        }

        return BridgeLaunch(
            inputPath = inputPath,
            targetComponents = targetComponents,
            targetAction = sourceIntent.action,
        )
    }
}
