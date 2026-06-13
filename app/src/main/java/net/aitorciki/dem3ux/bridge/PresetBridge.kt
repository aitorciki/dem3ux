package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent

data class PresetBridge(
    val id: String,
    val aliasClassName: String,
    val targetActivities: List<String>,
    val inputExtraKey: String? = null,
) {
    constructor(
        id: String,
        aliasClassName: String,
        targetActivity: String,
        inputExtraKey: String? = null,
    ) : this(
        id = id,
        aliasClassName = aliasClassName,
        targetActivities = listOf(targetActivity),
        inputExtraKey = inputExtraKey,
    )

    val targetComponents: List<ComponentName> = targetActivities.mapNotNull(ComponentName::unflattenFromString)
    val targetComponent: ComponentName? = targetComponents.firstOrNull()

    fun inputPathFrom(sourceIntent: Intent): String? =
        if (inputExtraKey == null) {
            sourceIntent.dataString
        } else {
            sourceIntent.getStringExtra(inputExtraKey)
        }

    fun resolveTargetComponent(isTargetInstalled: (ComponentName) -> Boolean): ComponentName? =
        targetComponents.firstOrNull(isTargetInstalled)
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
                targetActivities =
                    listOf(
                        "com.flycast.emulator/com.flycast.emulator.MainActivity",
                        "com.flycast.emulator/com.reicast.emulator.MainActivity",
                    ),
            ),
        )

    private val presetsByAliasClassName by lazy {
        presets.associateBy { preset -> preset.aliasClassName }
    }

    fun fromAliasClassName(className: String): PresetBridge? = presetsByAliasClassName[className]

    private fun preset(preset: PresetBridge): PresetBridge = preset.also { registeredPreset -> presets += registeredPreset }
}
