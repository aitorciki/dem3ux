package net.aitorciki.dem3ux.bridge

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
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

        forwardExtras(
            sourceIntent = sourceIntent,
            targetIntent = targetIntent,
            inputPath = inputPath,
            selectedEntry = selectedEntry,
            embeddedExtraReplacement = embeddedExtraReplacement,
        )

        if (selectedEntry.startsWith("content://")) {
            targetIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetIntent.clipData = ClipData.newRawUri("dem3ux selected entry", selectedEntry.toUri())
        }

        return targetIntent
    }

    private fun forwardExtras(
        sourceIntent: Intent,
        targetIntent: Intent,
        inputPath: String,
        selectedEntry: String,
        embeddedExtraReplacement: EmbeddedExtraPattern?,
    ) {
        val sourceExtras = sourceIntent.extras ?: return
        val forwardedBundle = Bundle(sourceExtras)
        sourceExtras.keySet().orEmpty().forEach { key ->
            if (key.startsWith(DEM3UX_EXTRA_PREFIX)) {
                forwardedBundle.remove(key)
                return@forEach
            }

            @Suppress("DEPRECATION")
            when (val value = forwardedBundle.get(key)) {
                is String -> {
                    val replacement =
                        if (embeddedExtraReplacement?.key == key) {
                            embeddedExtraReplacement.replaceInputPath(extraValue = value, selectedEntry = selectedEntry)
                        } else if (value == inputPath) {
                            selectedEntry
                        } else {
                            value
                        }
                    if (replacement != value) {
                        forwardedBundle.putString(key, replacement)
                    }
                }

                is Array<*> -> {
                    if (value.all { it is String } && value.any { it == inputPath }) {
                        @Suppress("UNCHECKED_CAST")
                        forwardedBundle.putStringArray(
                            key,
                            (value as Array<String>).map { v -> if (v == inputPath) selectedEntry else v }.toTypedArray(),
                        )
                    }
                }
            }
        }
        targetIntent.replaceExtras(forwardedBundle)
    }

    private const val PROXIED_ACTIVITY_FLAGS =
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NO_HISTORY

    private const val DEM3UX_EXTRA_PREFIX = "dem3ux."
}
