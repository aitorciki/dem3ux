package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent

data class BridgePreset(
    val id: String,
    val aliasClassName: String,
    val targetActivity: String,
    val targetAction: String? = null,
    val inputExtraKey: String? = null,
) {
    val targetComponent: ComponentName? = ComponentName.unflattenFromString(targetActivity)

    fun inputPathFrom(sourceIntent: Intent): String? =
        if (inputExtraKey == null) {
            sourceIntent.dataString
        } else {
            sourceIntent.getStringExtra(inputExtraKey)
        }
}

object BridgePresets {
    val duckStation =
        BridgePreset(
            id = "duckstation",
            aliasClassName = "net.aitorciki.dem3ux.presets.DuckStationBridgeActivity",
            targetActivity = "com.github.stenzek.duckstation/.EmulationActivity",
            inputExtraKey = "bootPath",
        )

    fun fromAliasClassName(className: String): BridgePreset? =
        listOf(duckStation).firstOrNull { preset -> preset.aliasClassName == className }
}
