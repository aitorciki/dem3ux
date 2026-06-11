package net.aitorciki.dem3ux.bridge

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri

object BridgeTargetIntentFactory {
    fun build(
        sourceIntent: Intent,
        targetComponent: ComponentName,
        inputPath: String,
        selectedEntry: String,
    ): Intent {
        val targetIntent =
            Intent(sourceIntent.getStringExtra(BridgeContract.EXTRA_TARGET_ACTION)).apply {
                component = targetComponent
            }

        var replacedInputPath = false
        sourceIntent.extras?.keySet().orEmpty().forEach { key ->
            when {
                key.startsWith(BridgeContract.TARGET_EXTRA_PREFIX) -> {
                    val targetExtraName = key.removePrefix(BridgeContract.TARGET_EXTRA_PREFIX)
                    val value = sourceIntent.getExtraValue(key)
                    val forwardedValue = if (value == inputPath) selectedEntry else value
                    if (value == inputPath) {
                        replacedInputPath = true
                    }
                    putExtra(targetIntent, targetExtraName, forwardedValue)
                }

                key.startsWith(BridgeContract.TARGET_FLAG_PREFIX) && sourceIntent.getBooleanExtra(key, false) -> {
                    targetIntent.addFlags(flagFor(key))
                }
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
            is Boolean -> intent.putExtra(name, value)
            is Int -> intent.putExtra(name, value)
            is String -> intent.putExtra(name, value)
        }
    }

    private fun flagFor(key: String): Int =
        when (key) {
            BridgeContract.TARGET_FLAG_CLEAR_TASK -> Intent.FLAG_ACTIVITY_CLEAR_TASK
            BridgeContract.TARGET_FLAG_CLEAR_TOP -> Intent.FLAG_ACTIVITY_CLEAR_TOP
            BridgeContract.TARGET_FLAG_NO_HISTORY -> Intent.FLAG_ACTIVITY_NO_HISTORY
            else -> 0
        }
}
