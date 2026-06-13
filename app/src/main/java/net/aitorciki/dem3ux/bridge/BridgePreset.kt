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
    private val presets = mutableListOf<BridgePreset>()

    val duckStation =
        preset(
            BridgePreset(
                id = "duckstation",
                aliasClassName = "net.aitorciki.dem3ux.presets.DuckStationBridgeActivity",
                targetActivity = "com.github.stenzek.duckstation/.EmulationActivity",
                inputExtraKey = "bootPath",
            ),
        )

    val flycast =
        preset(
            BridgePreset(
                id = "flycast",
                aliasClassName = "net.aitorciki.dem3ux.presets.FlycastBridgeActivity",
                targetActivity = "com.flycast.emulator/com.flycast.emulator.MainActivity",
                targetAction = Intent.ACTION_VIEW,
            ),
        )

    private val presetsByAliasClassName by lazy {
        presets.associateBy { preset -> preset.aliasClassName }
    }

    fun fromAliasClassName(className: String): BridgePreset? = presetsByAliasClassName[className]

    private fun preset(preset: BridgePreset): BridgePreset = preset.also { registeredPreset -> presets += registeredPreset }
}
