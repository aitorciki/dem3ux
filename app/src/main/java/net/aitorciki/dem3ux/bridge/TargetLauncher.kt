package net.aitorciki.dem3ux.bridge

import android.content.Intent

sealed interface TargetLaunchResult {
    data object Success : TargetLaunchResult

    data class ActivityNotFound(
        val error: android.content.ActivityNotFoundException,
    ) : TargetLaunchResult

    data class Error(
        val error: Throwable,
    ) : TargetLaunchResult
}

interface TargetLauncher {
    fun launch(targetIntent: Intent): TargetLaunchResult
}
