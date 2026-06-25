package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent

data class PresetBridge(
    val id: String,
    val displayName: String = id,
    val aliasClassName: String,
    val targetActivities: List<String>,
    val inputExtraKey: String? = null,
    val inputExtraPatterns: List<EmbeddedExtraPattern> = emptyList(),
    val esDeEmulatorName: String? = null,
) {
    val targetComponents: List<ComponentName> =
        targetActivities.map { targetActivity ->
            requireNotNull(ComponentName.unflattenFromString(targetActivity)) {
                "Invalid preset target activity: $targetActivity"
            }
        }
    val targetComponent: ComponentName? = targetComponents.firstOrNull()
    val esDeAliasEntry: String = "net.aitorciki.dem3ux/${aliasClassName.removePrefix("net.aitorciki.dem3ux")}"

    fun inputFrom(sourceIntent: Intent): PresetBridgeInput? =
        when {
            inputExtraKey != null -> {
                sourceIntent.getStringExtra(inputExtraKey)?.let { inputPath -> PresetBridgeInput(inputPath = inputPath) }
            }

            inputExtraPatterns.isNotEmpty() -> {
                inputExtraPatterns.firstNotNullOfOrNull { pattern ->
                    val extraValue = sourceIntent.getStringExtra(pattern.key) ?: return@firstNotNullOfOrNull null
                    pattern.extractInputPath(extraValue)?.let { inputPath ->
                        PresetBridgeInput(inputPath = inputPath, embeddedExtraReplacement = pattern)
                    }
                }
            }

            else -> {
                sourceIntent.dataString?.let { inputPath -> PresetBridgeInput(inputPath = inputPath) }
            }
        }

    fun inputPathFrom(sourceIntent: Intent): String? = inputFrom(sourceIntent)?.inputPath

    fun resolveTargetComponent(isTargetInstalled: (ComponentName) -> Boolean): ComponentName? =
        targetComponents.firstOrNull(isTargetInstalled)
}

data class PresetBridgeInput(
    val inputPath: String,
    val embeddedExtraReplacement: EmbeddedExtraPattern? = null,
)

data class EmbeddedExtraPattern(
    val key: String,
    val regex: String,
    val group: Int = 1,
) {
    fun extractInputPath(extraValue: String): String? {
        val match = Regex(regex).find(extraValue) ?: return null
        return match.groups[group]?.value
    }

    fun replaceInputPath(
        extraValue: String,
        selectedEntry: String,
    ): String {
        val match = Regex(regex).find(extraValue) ?: return extraValue
        val groupRange = match.groups[group]?.range ?: return extraValue
        return extraValue.replaceRange(groupRange, selectedEntry)
    }
}
