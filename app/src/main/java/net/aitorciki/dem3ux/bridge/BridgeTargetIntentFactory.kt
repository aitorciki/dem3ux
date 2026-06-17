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
        embeddedExtraReplacement: EmbeddedExtraPattern? = null,
    ): Intent {
        val targetIntent =
            Intent(targetAction).apply {
                component = targetComponent
                data =
                    sourceIntent.data?.let { data ->
                        if (data.toString() == inputPath) {
                            selectedEntry.toUri()
                        } else {
                            data
                        }
                    }
                addFlags(sourceIntent.flags and PROXIED_ACTIVITY_FLAGS)
                sourceIntent.categories.orEmpty().forEach(::addCategory)
            }

        sourceIntent.extras?.keySet().orEmpty().forEach { key ->
            if (!key.startsWith(DEM3UX_EXTRA_PREFIX)) {
                val value = sourceIntent.getExtraValue(key)
                val replacement =
                    value.replaceInputPath(
                        key = key,
                        inputPath = inputPath,
                        selectedEntry = selectedEntry,
                        embeddedExtraReplacement = embeddedExtraReplacement,
                    )
                putExtra(targetIntent, key, replacement)
            }
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
        key: String,
        inputPath: String,
        selectedEntry: String,
        embeddedExtraReplacement: EmbeddedExtraPattern?,
    ): Any? =
        when (this) {
            is String if embeddedExtraReplacement?.key == key -> {
                embeddedExtraReplacement.replaceInputPath(extraValue = this, selectedEntry = selectedEntry)
            }

            inputPath -> {
                selectedEntry
            }

            is Array<*> if all { it is String } -> {
                map { value ->
                    if (value == inputPath) {
                        selectedEntry
                    } else {
                        value as String
                    }
                }.toTypedArray()
            }

            else -> {
                this
            }
        }

    private const val PROXIED_ACTIVITY_FLAGS =
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NO_HISTORY

    private const val DEM3UX_EXTRA_PREFIX = "dem3ux."
}
