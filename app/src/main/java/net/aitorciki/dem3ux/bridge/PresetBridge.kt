package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent

data class PresetBridge(
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

object PresetBridges {
    private val presets = mutableListOf<PresetBridge>()

    val duckStation =
        preset(
            PresetBridge(
                id = "duckstation",
                aliasClassName = "net.aitorciki.dem3ux.presets.DuckStationBridgeActivity",
                targetActivity = "com.github.stenzek.duckstation/.EmulationActivity",
                inputExtraKey = "bootPath",
            ),
        )

    val flycast =
        preset(
            PresetBridge(
                id = "flycast",
                aliasClassName = "net.aitorciki.dem3ux.presets.FlycastBridgeActivity",
                targetActivity = "com.flycast.emulator/com.flycast.emulator.MainActivity",
                targetAction = Intent.ACTION_VIEW,
            ),
        )

    private val presetsByAliasClassName by lazy {
        presets.associateBy { preset -> preset.aliasClassName }
    }

    fun fromAliasClassName(className: String): PresetBridge? = presetsByAliasClassName[className]

    private fun preset(preset: PresetBridge): PresetBridge = preset.also { registeredPreset -> presets += registeredPreset }
}
