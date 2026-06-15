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
