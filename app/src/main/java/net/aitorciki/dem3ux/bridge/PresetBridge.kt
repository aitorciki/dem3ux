package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent

data class PresetBridge(
    val id: String,
    val displayName: String = id,
    val aliasClassName: String,
    val targetActivities: List<String>,
    val inputExtraKey: String? = null,
    val esDeEmulatorName: String? = null,
) {
    constructor(
        id: String,
        displayName: String,
        aliasClassName: String,
        targetActivity: String,
        inputExtraKey: String? = null,
        esDeEmulatorName: String? = null,
    ) : this(
        id = id,
        displayName = displayName,
        aliasClassName = aliasClassName,
        targetActivities = listOf(targetActivity),
        inputExtraKey = inputExtraKey,
        esDeEmulatorName = esDeEmulatorName,
    )

    val targetComponents: List<ComponentName> = targetActivities.mapNotNull(ComponentName::unflattenFromString)
    val targetComponent: ComponentName? = targetComponents.firstOrNull()
    val esDeAliasEntry: String = "net.aitorciki.dem3ux/${aliasClassName.removePrefix("net.aitorciki.dem3ux")}"

    fun inputPathFrom(sourceIntent: Intent): String? =
        if (inputExtraKey == null) {
            sourceIntent.dataString
        } else {
            sourceIntent.getStringExtra(inputExtraKey)
        }

    fun resolveTargetComponent(isTargetInstalled: (ComponentName) -> Boolean): ComponentName? =
        targetComponents.firstOrNull(isTargetInstalled)
}
