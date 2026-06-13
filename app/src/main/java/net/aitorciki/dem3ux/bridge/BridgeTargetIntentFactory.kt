package net.aitorciki.dem3ux.bridge

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri

object BridgeTargetIntentFactory {
    fun build(
        sourceIntent: Intent,
        targetComponent: ComponentName,
        targetAction: String? = null,
        inputPath: String,
        selectedEntry: String,
    ): Intent {
        val resolvedTargetAction = targetAction ?: sourceIntent.getStringExtra(BridgeContract.EXTRA_TARGET_ACTION)
        val targetIntent =
            Intent(resolvedTargetAction).apply {
                component = targetComponent
                addFlags(sourceIntent.flags and PROXIED_ACTIVITY_FLAGS)
                sourceIntent.categories.orEmpty().forEach(::addCategory)
            }

        var replacedInputPath = false
        sourceIntent.extras?.keySet().orEmpty().forEach { key ->
            if (!key.startsWith(DEM3UX_EXTRA_PREFIX)) {
                val value = sourceIntent.getExtraValue(key)
                val replacement = value.replaceInputPath(inputPath = inputPath, selectedEntry = selectedEntry)
                if (replacement.replacedInputPath) {
                    replacedInputPath = true
                }
                putExtra(targetIntent, key, replacement.value)
            }
        }

        if (!replacedInputPath) {
            targetIntent.data = selectedEntry.toUri()
        }

        if (selectedEntry.startsWith("content://")) {
            targetIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetIntent.clipData = ClipData.newRawUri("dem3ux selected entry", selectedEntry.toUri())
        }

        return targetIntent
    }

    @Suppress("DEPRECATION")
    private fun Intent.getExtraValue(key: String): Any? = extras?.get(key)

    private fun putExtra(
        intent: Intent,
        name: String,
        value: Any?,
    ) {
        when (value) {
            is Boolean -> {
                intent.putExtra(name, value)
            }

            is Int -> {
                intent.putExtra(name, value)
            }

            is String -> {
                intent.putExtra(name, value)
            }

            is Array<*> -> {
                if (value.all { it is String }) {
                    @Suppress("UNCHECKED_CAST")
                    intent.putExtra(name, value as Array<String>)
                }
            }
        }
    }

    private fun Any?.replaceInputPath(
        inputPath: String,
        selectedEntry: String,
    ): ExtraReplacement =
        when (this) {
            inputPath -> {
                ExtraReplacement(value = selectedEntry, replacedInputPath = true)
            }

            is Array<*> if all { it is String } -> {
                var replacedInputPath = false
                val values =
                    map { value ->
                        if (value == inputPath) {
                            replacedInputPath = true
                            selectedEntry
                        } else {
                            value as String
                        }
                    }.toTypedArray()

                ExtraReplacement(value = values, replacedInputPath = replacedInputPath)
            }

            else -> {
                ExtraReplacement(value = this, replacedInputPath = false)
            }
        }

    private const val PROXIED_ACTIVITY_FLAGS =
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NO_HISTORY

    private const val DEM3UX_EXTRA_PREFIX = "dem3ux."

    private data class ExtraReplacement(
        val value: Any?,
        val replacedInputPath: Boolean,
    )
}
