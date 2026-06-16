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
