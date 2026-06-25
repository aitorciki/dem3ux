package net.aitorciki.dem3ux

import android.content.Intent
import net.aitorciki.dem3ux.bridge.BridgeLaunch
import net.aitorciki.dem3ux.bridge.PresetBridges

class PresetBridgeActivity : BaseBridgeActivity() {
    override fun createBridgeLaunch(sourceIntent: Intent): BridgeLaunch? {
        val aliasClassName = sourceIntent.component?.className ?: componentName.className
        return createPresetBridgeLaunch(sourceIntent = sourceIntent, aliasClassName = aliasClassName)
    }
}

internal fun createPresetBridgeLaunch(
    sourceIntent: Intent,
    aliasClassName: String,
): BridgeLaunch? {
    val preset = PresetBridges.fromAliasClassName(aliasClassName) ?: return null
    val input = preset.inputFrom(sourceIntent)
    val targetComponents = preset.targetComponents

    if (targetComponents.isEmpty() || input == null || input.inputPath.raw.isBlank()) {
        return null
    }

    return BridgeLaunch(
        inputPath = input.inputPath,
        targetComponents = targetComponents,
        targetAction = sourceIntent.action,
        embeddedExtraReplacement = input.embeddedExtraReplacement,
    )
}
